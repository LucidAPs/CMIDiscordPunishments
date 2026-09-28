package com.lucidaps.cmidiscordpunishments.discord;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.lucidaps.cmidiscordpunishments.TestSettings;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReport;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookDispatcherTest {
    private HttpServer server;
    private WebhookDispatcher dispatcher;

    @AfterEach
    void tearDown() {
        if (dispatcher != null) {
            dispatcher.close(Duration.ofMillis(200));
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsJsonPayload() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        startServer(exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 204, "");
        });

        DeliveryResult result = createDispatcher().submit(report("Spam")).get(3, TimeUnit.SECONDS);
        assertEquals(DeliveryStatus.DELIVERED, result.status());
        assertTrue(body.get().contains("Player Warned"));
        assertTrue(body.get().contains("Spam"));
    }

    @Test
    void retriesRateLimitsAndServerErrorsInOrder() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        List<String> bodies = new ArrayList<>();
        startServer(exchange -> {
            synchronized (bodies) {
                bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
            int attempt = requests.incrementAndGet();
            if (attempt == 1) {
                respond(exchange, 429, "{\"retry_after\":0.01}");
            } else if (attempt == 2) {
                respond(exchange, 503, "try again");
            } else {
                respond(exchange, 204, "");
            }
        });

        DeliveryResult first = createDispatcher().submit(report("First")).get(3, TimeUnit.SECONDS);
        DeliveryResult second = dispatcher.submit(report("Second")).get(3, TimeUnit.SECONDS);

        assertEquals(DeliveryStatus.DELIVERED, first.status());
        assertEquals(DeliveryStatus.DELIVERED, second.status());
        assertEquals(4, requests.get());
        assertTrue(bodies.get(0).contains("First"));
        assertTrue(bodies.get(1).contains("First"));
        assertTrue(bodies.get(2).contains("First"));
        assertTrue(bodies.get(3).contains("Second"));
    }

    @Test
    void doesNotRetryClientErrors() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        startServer(exchange -> {
            requests.incrementAndGet();
            respond(exchange, 400, "bad request");
        });

        DeliveryResult result = createDispatcher().submit(report("Bad")).get(3, TimeUnit.SECONDS);
        assertEquals(DeliveryStatus.FAILED, result.status());
        assertEquals(400, result.httpStatus());
        assertEquals(1, requests.get());
    }

    @Test
    void rejectsReportsWhenDisabled() throws Exception {
        dispatcher = new WebhookDispatcher(TestSettings.create(null), Logger.getAnonymousLogger());
        DeliveryResult result = dispatcher.submit(report("Disabled")).get(1, TimeUnit.SECONDS);
        assertEquals(DeliveryStatus.DISABLED, result.status());
    }

    @Test
    void boundsTheInMemoryQueue() throws Exception {
        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch releaseRequest = new CountDownLatch(1);
        startServer(exchange -> {
            requestStarted.countDown();
            try {
                releaseRequest.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 204, "");
        });
        dispatcher = new WebhookDispatcher(
            TestSettings.create(serverUri(), 1),
            Logger.getAnonymousLogger(),
            new DiscordEmbedRenderer(),
            Duration.ofMillis(10)
        );

        var first = dispatcher.submit(report("First"));
        assertTrue(requestStarted.await(1, TimeUnit.SECONDS));
        DeliveryResult rejected = dispatcher.submit(report("Second")).get(1, TimeUnit.SECONDS);
        releaseRequest.countDown();

        assertEquals(DeliveryStatus.QUEUE_FULL, rejected.status());
        assertEquals(DeliveryStatus.DELIVERED, first.get(2, TimeUnit.SECONDS).status());
    }

    private WebhookDispatcher createDispatcher() {
        PluginSettings settings = TestSettings.create(serverUri());
        dispatcher = new WebhookDispatcher(
            settings,
            Logger.getAnonymousLogger(),
            new DiscordEmbedRenderer(),
            Duration.ofMillis(10)
        );
        return dispatcher;
    }

    private PunishmentReport report(String reason) {
        return PunishmentReport.builder(PunishmentType.WARN)
            .target("Alice")
            .actor("Moderator")
            .reason(reason)
            .build();
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/webhook", exchange -> handler.handle(exchange));
        server.start();
    }

    private URI serverUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/webhook");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
