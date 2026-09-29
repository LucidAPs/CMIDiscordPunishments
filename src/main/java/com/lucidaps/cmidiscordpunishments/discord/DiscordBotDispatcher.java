package com.lucidaps.cmidiscordpunishments.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lucidaps.cmidiscordpunishments.config.DiscordDestination;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiscordBotDispatcher implements AutoCloseable {
    private static final URI DISCORD_API_BASE = URI.create("https://discord.com/api/v10/");
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(1);
    private static final long QUEUE_WARNING_INTERVAL_MILLIS = 60_000L;

    private final Logger logger;
    private final DiscordEmbedRenderer renderer;
    private final Duration retryBaseDelay;
    private final URI apiBaseUri;
    private final AtomicReference<PluginSettings> settings;
    private final ScheduledThreadPoolExecutor executor;
    private final Deque<QueuedReport> queue = new ArrayDeque<>();
    private final AtomicBoolean workerActive = new AtomicBoolean();
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicLong lastQueueWarning = new AtomicLong();
    private volatile HttpClient httpClient;
    private volatile Duration clientConnectTimeout;

    public DiscordBotDispatcher(PluginSettings initialSettings, Logger logger) {
        this(initialSettings, logger, new DiscordEmbedRenderer(), DEFAULT_RETRY_DELAY, DISCORD_API_BASE);
    }

    DiscordBotDispatcher(
        PluginSettings initialSettings,
        Logger logger,
        DiscordEmbedRenderer renderer,
        Duration retryBaseDelay,
        URI apiBaseUri
    ) {
        this.settings = new AtomicReference<>(Objects.requireNonNull(initialSettings, "initialSettings"));
        this.logger = Objects.requireNonNull(logger, "logger");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.retryBaseDelay = Objects.requireNonNull(retryBaseDelay, "retryBaseDelay");
        this.apiBaseUri = withTrailingSlash(Objects.requireNonNull(apiBaseUri, "apiBaseUri"));
        this.clientConnectTimeout = initialSettings.connectTimeout();
        this.httpClient = buildClient(clientConnectTimeout);
        this.executor = new ScheduledThreadPoolExecutor(1, daemonThreadFactory());
        this.executor.setRemoveOnCancelPolicy(true);
    }

    public void updateSettings(PluginSettings updatedSettings) {
        Objects.requireNonNull(updatedSettings, "updatedSettings");
        if (!updatedSettings.connectTimeout().equals(clientConnectTimeout)) {
            clientConnectTimeout = updatedSettings.connectTimeout();
            httpClient = buildClient(clientConnectTimeout);
        }
        settings.set(updatedSettings);
        startWorker();
    }

    public CompletableFuture<DeliveryResult> submit(PunishmentReport report) {
        Objects.requireNonNull(report, "report");
        PluginSettings current = settings.get();
        if (!current.style(report.type()).enabled()) {
            return CompletableFuture.completedFuture(
                new DeliveryResult(DeliveryStatus.FILTERED, 0, "This action is disabled in config.yml")
            );
        }
        if (current.discord().isEmpty()) {
            return CompletableFuture.completedFuture(
                new DeliveryResult(DeliveryStatus.DISABLED, 0, "No Discord bot destination is configured")
            );
        }
        if (!accepting.get()) {
            return CompletableFuture.completedFuture(
                new DeliveryResult(DeliveryStatus.SHUTDOWN, 0, "Discord dispatcher is shutting down")
            );
        }

        QueuedReport queued = new QueuedReport(report);
        synchronized (queue) {
            if (queue.size() >= current.queueCapacity()) {
                warnQueueFull();
                return CompletableFuture.completedFuture(
                    new DeliveryResult(DeliveryStatus.QUEUE_FULL, 0, "Discord delivery queue is full")
                );
            }
            queue.addLast(queued);
        }
        startWorker();
        return queued.completion;
    }

    public int queuedCount() {
        synchronized (queue) {
            return queue.size();
        }
    }

    public void close(Duration drainTimeout) {
        accepting.set(false);
        long deadline = System.nanoTime() + Math.max(0L, drainTimeout.toNanos());
        startWorker();
        while (System.nanoTime() < deadline) {
            if (queuedCount() == 0 && !workerActive.get()) {
                break;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        executor.shutdownNow();
        synchronized (queue) {
            QueuedReport report;
            while ((report = queue.pollFirst()) != null) {
                report.completion.complete(
                    new DeliveryResult(DeliveryStatus.SHUTDOWN, 0, "Plugin disabled before delivery completed")
                );
            }
        }
    }

    @Override
    public void close() {
        close(Duration.ofSeconds(2));
    }

    private void startWorker() {
        if (!workerActive.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.execute(this::workQueue);
        } catch (RejectedExecutionException exception) {
            workerActive.set(false);
        }
    }

    private void workQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            QueuedReport item = peek();
            if (item == null) {
                workerActive.set(false);
                if (peek() != null && workerActive.compareAndSet(false, true)) {
                    continue;
                }
                return;
            }

            AttemptResult result = attempt(item);
            if (result.retryDelay != null && accepting.get()) {
                item.attempts++;
                try {
                    executor.schedule(this::workQueue, result.retryDelay.toMillis(), TimeUnit.MILLISECONDS);
                } catch (RejectedExecutionException exception) {
                    finish(item, new DeliveryResult(DeliveryStatus.SHUTDOWN, 0, "Dispatcher stopped during retry"));
                    workerActive.set(false);
                }
                return;
            }
            finish(item, result.deliveryResult);
        }
        workerActive.set(false);
    }

    private AttemptResult attempt(QueuedReport item) {
        PluginSettings current = settings.get();
        Optional<DiscordDestination> configured = current.discord();
        if (configured.isEmpty()) {
            return AttemptResult.done(DeliveryStatus.DISABLED, 0, "No Discord bot destination is configured");
        }

        DiscordDestination destination = configured.get();
        try {
            String body = renderer.render(item.report, current);
            HttpRequest request = HttpRequest.newBuilder(messageEndpoint(destination.channelId()))
                .timeout(current.requestTimeout())
                .header("Authorization", "Bot " + destination.botToken())
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "CMIDiscordPunishments/1.0.0")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return AttemptResult.done(DeliveryStatus.DELIVERED, status, "Discord accepted the report");
            }
            if (status == 429 && canRetry(item, current)) {
                return AttemptResult.retry(retryAfter(response));
            }
            if (status >= 500 && status < 600 && canRetry(item, current)) {
                return AttemptResult.retry(exponentialDelay(item.attempts));
            }
            logger.warning("Discord rejected a punishment report with HTTP " + status + ".");
            return AttemptResult.done(DeliveryStatus.FAILED, status, clientFailureMessage(status));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return AttemptResult.done(DeliveryStatus.SHUTDOWN, 0, "Delivery was interrupted");
        } catch (IOException exception) {
            if (canRetry(item, current)) {
                logger.log(Level.FINE, "Discord bot delivery failed and will be retried.", exception);
                return AttemptResult.retry(exponentialDelay(item.attempts));
            }
            logger.log(Level.WARNING, "Discord bot delivery failed after all retries: " + exception.getMessage());
            return AttemptResult.done(DeliveryStatus.FAILED, 0, "Network error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            logger.warning("Could not build or send a Discord punishment report ("
                + exception.getClass().getSimpleName() + ").");
            return AttemptResult.done(DeliveryStatus.FAILED, 0, "Could not build or send the Discord report");
        }
    }

    private URI messageEndpoint(String channelId) {
        return apiBaseUri.resolve("channels/" + channelId + "/messages");
    }

    private String clientFailureMessage(int status) {
        return switch (status) {
            case 401 -> "Discord rejected the configured bot token (HTTP 401)";
            case 403 -> "Discord denied the bot permission to send embeds in the configured channel (HTTP 403)";
            case 404 -> "The configured Discord channel was not found or is inaccessible (HTTP 404)";
            default -> "Discord returned HTTP " + status;
        };
    }

    private boolean canRetry(QueuedReport item, PluginSettings current) {
        return accepting.get() && item.attempts < current.maxRetries();
    }

    private Duration exponentialDelay(int completedRetries) {
        long multiplier = 1L << Math.min(completedRetries, 10);
        return retryBaseDelay.multipliedBy(multiplier);
    }

    private Duration retryAfter(HttpResponse<String> response) {
        double seconds = parseRetryAfterBody(response.body());
        if (seconds <= 0) {
            seconds = response.headers().firstValue("Retry-After").map(this::parseDouble).orElse(0D);
        }
        if (seconds <= 0) {
            return retryBaseDelay;
        }
        long millis = Math.max(50L, Math.min(60_000L, (long) Math.ceil(seconds * 1_000D)));
        return Duration.ofMillis(millis);
    }

    private double parseRetryAfterBody(String body) {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonObject()) {
                JsonObject object = parsed.getAsJsonObject();
                JsonElement retryAfter = object.get("retry_after");
                if (retryAfter != null && retryAfter.isJsonPrimitive()) {
                    return retryAfter.getAsDouble();
                }
            }
        } catch (RuntimeException ignored) {
            // Fall back to the HTTP header or the normal retry delay.
        }
        return 0D;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0D;
        }
    }

    private QueuedReport peek() {
        synchronized (queue) {
            return queue.peekFirst();
        }
    }

    private void finish(QueuedReport expected, DeliveryResult result) {
        synchronized (queue) {
            if (queue.peekFirst() == expected) {
                queue.removeFirst();
            } else {
                queue.remove(expected);
            }
        }
        expected.completion.complete(result);
    }

    private void warnQueueFull() {
        long now = System.currentTimeMillis();
        long previous = lastQueueWarning.get();
        if (now - previous >= QUEUE_WARNING_INTERVAL_MILLIS && lastQueueWarning.compareAndSet(previous, now)) {
            logger.warning("Discord delivery queue is full; new punishment reports are being dropped.");
        }
    }

    private static URI withTrailingSlash(URI uri) {
        String value = uri.toString();
        return value.endsWith("/") ? uri : URI.create(value + "/");
    }

    private static HttpClient buildClient(Duration connectTimeout) {
        return HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "CMIDiscordPunishments-DiscordBot");
            thread.setDaemon(true);
            return thread;
        };
    }

    private static final class QueuedReport {
        private final PunishmentReport report;
        private final CompletableFuture<DeliveryResult> completion = new CompletableFuture<>();
        private int attempts;

        private QueuedReport(PunishmentReport report) {
            this.report = report;
        }
    }

    private record AttemptResult(DeliveryResult deliveryResult, Duration retryDelay) {
        private static AttemptResult done(DeliveryStatus status, int httpStatus, String message) {
            return new AttemptResult(new DeliveryResult(status, httpStatus, message), null);
        }

        private static AttemptResult retry(Duration delay) {
            return new AttemptResult(null, delay);
        }
    }
}
