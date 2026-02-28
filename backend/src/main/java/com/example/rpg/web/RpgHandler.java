package com.example.rpg.web;

import com.example.rpg.game.GameService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class RpgHandler implements HttpHandler {
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,128}$");

    private final SessionGameStore sessionStore;
    private final Set<String> allowOrigins;
    private final boolean allowAnyOrigin;
    private final String allowMethods;
    private final String allowHeaders;
    private final boolean allowCredentials;
    private final String sessionCookieName;

    public RpgHandler(
            SessionGameStore sessionStore,
            String corsAllowOrigins,
            String corsAllowMethods,
            String corsAllowHeaders,
            boolean corsAllowCredentials,
            String sessionCookieName
    ) {
        this.sessionStore = sessionStore;
        this.allowOrigins = parseOrigins(corsAllowOrigins);
        this.allowAnyOrigin = this.allowOrigins.contains("*");
        this.allowMethods = corsAllowMethods;
        this.allowHeaders = corsAllowHeaders;
        this.allowCredentials = corsAllowCredentials;
        this.sessionCookieName = (sessionCookieName == null || sessionCookieName.isBlank())
                ? "RPG_SESSION"
                : sessionCookieName.trim();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        SessionContext sessionContext = resolveSession(exchange);
        GameService gameService = sessionContext.gameService;
        if (sessionContext.newSessionIssued) {
            exchange.getResponseHeaders().add("Set-Cookie", buildSessionCookie(exchange, sessionContext.sessionId));
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        Map<String, String> query = parseQuery(uri.getRawQuery());

        try {
            if ("GET".equalsIgnoreCase(method) && path.endsWith("/start")) {
                sendJson(exchange, 200, gameService.start());
                return;
            }

            if ("GET".equalsIgnoreCase(method) && path.endsWith("/begin")) {
                sendJson(exchange, 200, gameService.begin());
                return;
            }

            if ("GET".equalsIgnoreCase(method) && path.endsWith("/state")) {
                sendJson(exchange, 200, gameService.currentStateJson());
                return;
            }

            if (isGetOrPost(method) && path.endsWith("/map/enter")) {
                String node = query.getOrDefault("node", "");
                sendJson(exchange, 200, gameService.enterNode(node));
                return;
            }

            if (isGetOrPost(method) && path.endsWith("/map/recover")) {
                String item = query.getOrDefault("item", "");
                sendJson(exchange, 200, gameService.useMapItem(item));
                return;
            }

            if (isGetOrPost(method) && path.endsWith("/shop/open")) {
                sendJson(exchange, 200, gameService.openShop());
                return;
            }

            if (isGetOrPost(method) && path.endsWith("/shop/close")) {
                sendJson(exchange, 200, gameService.closeShop());
                return;
            }

            if (isGetOrPost(method) && path.endsWith("/shop/buy")) {
                String item = query.getOrDefault("item", "");
                sendJson(exchange, 200, gameService.buyItem(item));
                return;
            }

            if (isGetOrPost(method) && path.endsWith("/menu")) {
                String view = query.getOrDefault("view", "main");
                sendJson(exchange, 200, gameService.setMenu(view));
                return;
            }

            if (isGetOrPost(method) && path.endsWith("/action")) {
                String type = query.getOrDefault("type", "");
                String arg = query.getOrDefault("arg", "");
                sendJson(exchange, 200, gameService.action(type, arg));
                return;
            }

            if (isGetOrPost(method) && path.endsWith("/battle/continue")) {
                sendJson(exchange, 200, gameService.continueBattleResult());
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private boolean isGetOrPost(String method) {
        return "GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method);
    }

    private SessionContext resolveSession(HttpExchange exchange) {
        String sessionId = parseCookie(exchange.getRequestHeaders().getFirst("Cookie"), sessionCookieName);
        boolean valid = isValidSessionId(sessionId);
        boolean newSessionIssued = !valid;
        if (!valid) {
            sessionId = sessionStore.newSessionId();
        }
        GameService gameService = sessionStore.getOrCreate(sessionId);
        return new SessionContext(sessionId, gameService, newSessionIssued);
    }

    private String parseCookie(String rawCookieHeader, String name) {
        if (rawCookieHeader == null || rawCookieHeader.isBlank()) {
            return null;
        }
        String[] cookies = rawCookieHeader.split(";");
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            int delimiter = trimmed.indexOf('=');
            if (delimiter <= 0) {
                continue;
            }
            String cookieName = trimmed.substring(0, delimiter).trim();
            if (!cookieName.equals(name)) {
                continue;
            }
            return trimmed.substring(delimiter + 1).trim();
        }
        return null;
    }

    private boolean isValidSessionId(String sessionId) {
        return sessionId != null && SESSION_ID_PATTERN.matcher(sessionId).matches();
    }

    private String buildSessionCookie(HttpExchange exchange, String sessionId) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(sessionCookieName).append("=").append(sessionId);
        cookie.append("; Path=/");
        cookie.append("; HttpOnly");
        cookie.append("; SameSite=Lax");
        cookie.append("; Max-Age=").append(sessionStore.ttlSeconds());
        if (isSecureRequest(exchange)) {
            cookie.append("; Secure");
        }
        return cookie.toString();
    }

    private boolean isSecureRequest(HttpExchange exchange) {
        String forwardedProto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            return forwardedProto.toLowerCase(Locale.ROOT).contains("https");
        }
        return false;
    }

    private void addCorsHeaders(HttpExchange exchange) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        String allowOrigin = resolveAllowedOrigin(requestOrigin);
        if (allowOrigin != null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowOrigin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", allowMethods);
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", allowHeaders);
        if (allowCredentials && allowOrigin != null && !"*".equals(allowOrigin)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        }
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    }

    private String resolveAllowedOrigin(String requestOrigin) {
        if (allowAnyOrigin) {
            return "*";
        }
        if (requestOrigin == null || requestOrigin.isBlank()) {
            return null;
        }
        String normalized = requestOrigin.trim().toLowerCase(Locale.ROOT);
        return allowOrigins.contains(normalized) ? requestOrigin.trim() : null;
    }

    private Set<String> parseOrigins(String rawOrigins) {
        if (rawOrigins == null || rawOrigins.isBlank()) {
            return Set.of("*");
        }
        Set<String> result = new HashSet<>();
        Arrays.stream(rawOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    if ("*".equals(s)) {
                        result.add("*");
                    } else {
                        result.add(s.toLowerCase(Locale.ROOT));
                    }
                });
        if (result.isEmpty()) {
            result.add("*");
        }
        return result;
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1
                    ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                    : "";
            map.put(key, value);
        }
        return map;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private record SessionContext(String sessionId, GameService gameService, boolean newSessionIssued) {}
}
