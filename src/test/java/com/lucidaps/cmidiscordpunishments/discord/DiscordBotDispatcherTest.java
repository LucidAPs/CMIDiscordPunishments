package com.lucidaps.cmidiscordpunishments.discord;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.lucidaps.cmidiscordpunishments.TestSettings;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordBotDispatcherTest {
    private HttpServer server;
    private DiscordBotDispatcher dispatcher;

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
    void sendsBotAuthorizedJsonPayloadToConfiguredChannel() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        startServer(exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{}");
        });

        DeliveryResult result = createDispatcher().submit(report("Spam")).get(3, TimeUnit.SECONDS);

        assertEquals(DeliveryStatus.DELIVERED, result.status());
        assertEquals("POST", method.get());
        assertEquals("/api/v10/channels/" + TestSettings.CHANNEL_ID + "/messages", path.get());
        assertEquals("Bot " + TestSettings.BOT_TOKEN, authorization.get());
        assertTrue(body.get().contains("Alice WARNED"));
        assertTrue(body.get().contains("Spam"));
        assertFalse(body.get().contains("username"));
        assertFalse(body.get().contains("avatar_url"));
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
                respond(exchange, 200, "{}");
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
        assertEquals("Discord returned HTTP 400", result.message());
        assertEquals(1, requests.get());
    }

    @Test
    void returnsSafeDiagnosticsForBotAuthenticationAndChannelErrors() throws Exception {
        int[] statuses = {401, 403, 404};
        String[] messages = {
            "bot token",
            "permission",
            "channel was not found"
        };
        AtomicInteger requests = new AtomicInteger();
        startServer(exchange -> respond(exchange, statuses[requests.getAndIncrement()], "rejected"));
        createDispatcher();

        for (int index = 0; index < statuses.length; index++) {
            DeliveryResult result = dispatcher.submit(report("Failure " + index)).get(3, TimeUnit.SECONDS);
            assertEquals(DeliveryStatus.FAILED, result.status());
            assertEquals(statuses[index], result.httpStatus());
            assertTrue(result.message().contains(messages[index]));
            assertFalse(result.message().contains(TestSettings.BOT_TOKEN));
        }
        assertEquals(3, requests.get());
    }

    @Test
    void usesUpdatedTokenAndChannelForNewDeliveries() throws Exception {
        List<String> paths = new ArrayList<>();
        List<String> authorizations = new ArrayList<>();
        startServer(exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{}");
        });
        createDispatcher();

        dispatcher.submit(report("Before reload")).get(3, TimeUnit.SECONDS);
        dispatcher.updateSettings(TestSettings.create("updated-token", "987654321098765432"));
        dispatcher.submit(report("After reload")).get(3, TimeUnit.SECONDS);

        assertEquals(List.of(
            "/api/v10/channels/" + TestSettings.CHANNEL_ID + "/messages",
            "/api/v10/channels/987654321098765432/messages"
        ), paths);
        assertEquals(List.of("Bot " + TestSettings.BOT_TOKEN, "Bot updated-token"), authorizations);
    }

    @Test
    void rejectsReportsWhenDisabled() throws Exception {
        dispatcher = new DiscordBotDispatcher(TestSettings.disabled(), Logger.getAnonymousLogger());
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
            respond(exchange, 200, "{}");
        });
        dispatcher = new DiscordBotDispatcher(
            TestSettings.create(1),
            Logger.getAnonymousLogger(),
            new DiscordEmbedRenderer(),
            Duration.ofMillis(10),
            apiBaseUri()
        );

        var first = dispatcher.submit(report("First"));
        assertTrue(requestStarted.await(1, TimeUnit.SECONDS));
        DeliveryResult rejected = dispatcher.submit(report("Second")).get(1, TimeUnit.SECONDS);
        releaseRequest.countDown();

        assertEquals(DeliveryStatus.QUEUE_FULL, rejected.status());
        assertEquals(DeliveryStatus.DELIVERED, first.get(2, TimeUnit.SECONDS).status());
    }

    private DiscordBotDispatcher createDispatcher() {
        dispatcher = new DiscordBotDispatcher(
            TestSettings.create(),
            Logger.getAnonymousLogger(),
            new DiscordEmbedRenderer(),
            Duration.ofMillis(10),
            apiBaseUri()
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
        server.createContext("/", exchange -> handler.handle(exchange));
        server.start();
    }

    private URI apiBaseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v10/");
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
