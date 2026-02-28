package com.example.rpg;

import com.example.rpg.web.RpgHandler;
import com.example.rpg.web.SessionGameStore;
import com.example.rpg.web.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

public class Application {
    public static void main(String[] args) throws IOException {
        int port = envInt("APP_PORT", 8080);
        String corsOrigins = env("CORS_ALLOW_ORIGINS", "*");
        String corsMethods = env("CORS_ALLOW_METHODS", "GET,POST,OPTIONS");
        String corsHeaders = env("CORS_ALLOW_HEADERS", "Content-Type");
        boolean corsCredentials = envBool("CORS_ALLOW_CREDENTIALS", false);
        String sessionCookieName = env("SESSION_COOKIE_NAME", "RPG_SESSION");
        int sessionTtlMinutes = Math.max(5, envInt("SESSION_TTL_MINUTES", 120));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        SessionGameStore sessionStore = new SessionGameStore(Duration.ofMinutes(sessionTtlMinutes));

        server.createContext("/api/game", new RpgHandler(
                sessionStore,
                corsOrigins,
                corsMethods,
                corsHeaders,
                corsCredentials,
                sessionCookieName
        ));
        server.createContext("/healthz", exchange -> {
            byte[] bytes = "{\"status\":\"ok\"}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/", new StaticFileHandler(resolveWebRoot()));
        server.setExecutor(null);

        System.out.println("Simple RPG started: http://localhost:" + port);
        System.out.println("Health check: http://localhost:" + port + "/healthz");
        System.out.println("Session cookie: " + sessionCookieName + ", ttlMinutes=" + sessionTtlMinutes);
        server.start();
    }

    private static Path resolveWebRoot() {
        Path[] candidates = new Path[] {
                Paths.get("frontend", "public"),
                Paths.get("..", "frontend", "public")
        };

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) {
                return normalized;
            }
        }

        throw new IllegalStateException("frontend/public directory was not found.");
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static int envInt(String key, int defaultValue) {
        String raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static boolean envBool(String key, boolean defaultValue) {
        String raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "yes".equals(normalized)
                || "on".equals(normalized);
    }
}
