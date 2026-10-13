package com.scanner.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scanner.server.models.CheckoutRequest;
import com.scanner.server.models.CheckoutResponse;
import com.scanner.server.models.LicenseVerifyResponse;
import com.scanner.server.service.LemonSqueezyService;
import com.scanner.server.service.LicenseManager;
import com.scanner.server.service.StripeService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded HTTP server for Stripe Billing, Webhooks, and License Validation.
 * Default port: 4242.
 */
public class BillingServer {
    private static final Logger LOGGER = Logger.getLogger(BillingServer.class.getName());
    private static final int DEFAULT_PORT = 4242;

    private final int port;
    private final HttpServer server;
    private final ObjectMapper objectMapper;
    private final String activeProvider;
    private final LemonSqueezyService lemonSqueezyService;
    private final StripeService stripeService;
    private final LicenseManager licenseManager;

    public BillingServer(int port, Properties config) throws IOException {
        this.port = port;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.activeProvider = resolveConfig(config, "billing.provider", "BILLING_PROVIDER", "lemonsqueezy");

        String frontendUrl = resolveConfig(config, "app.frontend.url", "FRONTEND_URL", "http://localhost:3000");
        String licenseSecret = resolveConfig(config, "license.secret.key", "LICENSE_SECRET_KEY", "api-security-scanner-secret-hmac-key-2026");
        this.licenseManager = new LicenseManager("data/licenses.json", licenseSecret);

        // Lemon Squeezy configuration (Variant A - Merchant of Record)
        String lemonApiKey = resolveConfig(config, "lemonsqueezy.api.key", "LEMONSQUEEZY_API_KEY", "");
        String lemonStoreId = resolveConfig(config, "lemonsqueezy.store.id", "LEMONSQUEEZY_STORE_ID", "");
        String lemonWebhookSecret = resolveConfig(config, "lemonsqueezy.webhook.secret", "LEMONSQUEEZY_WEBHOOK_SECRET", "");
        Map<String, String> lemonVariants = Map.of(
                "starter", resolveConfig(config, "lemonsqueezy.variant.starter", "LEMON_VARIANT_STARTER", ""),
                "pro", resolveConfig(config, "lemonsqueezy.variant.pro", "LEMON_VARIANT_PRO", ""),
                "enterprise", resolveConfig(config, "lemonsqueezy.variant.enterprise", "LEMON_VARIANT_ENTERPRISE", "")
        );
        this.lemonSqueezyService = new LemonSqueezyService(lemonApiKey, lemonStoreId, lemonWebhookSecret, lemonVariants, frontendUrl, licenseManager);

        // Stripe configuration (Variant B)
        String stripeSecretKey = resolveConfig(config, "stripe.secret.key", "STRIPE_SECRET_KEY", "");
        String stripeWebhookSecret = resolveConfig(config, "stripe.webhook.secret", "STRIPE_WEBHOOK_SECRET", "");
        this.stripeService = new StripeService(stripeSecretKey, stripeWebhookSecret, frontendUrl, licenseManager);

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(16));

        setupRoutes();
    }

    private void setupRoutes() {
        server.createContext("/api/checkout/create-session", this::handleCreateCheckoutSession);
        server.createContext("/api/webhook/lemonsqueezy", this::handleLemonSqueezyWebhook);
        server.createContext("/api/webhook/stripe", this::handleStripeWebhook);
        server.createContext("/api/license/verify", this::handleLicenseVerify);
        server.createContext("/api/licenses", this::handleListLicenses);
        server.createContext("/api/health", this::handleHealthCheck);
    }

    public void start() {
        server.start();
        LOGGER.info("================================================================================");
        LOGGER.info("🚀 API Security Scanner Billing Server started on http://localhost:" + port);
        LOGGER.info("🌍 Active Payment Provider: " + activeProvider.toUpperCase(Locale.ROOT) + " (Variant A: Lemon Squeezy MoR)");
        LOGGER.info("🍋 Lemon Squeezy Mode: " + (lemonSqueezyService.isLiveConfigured() ? "LIVE (API Keys Active)" : "SANDBOX SIMULATION (Zero Setup Required)"));
        LOGGER.info("💳 Stripe Mode: " + (stripeService.isStripeLiveConfigured() ? "LIVE (API Keys Active)" : "SANDBOX SIMULATION"));
        LOGGER.info("📋 Endpoints:");
        LOGGER.info("   - POST http://localhost:" + port + "/api/checkout/create-session");
        LOGGER.info("   - POST http://localhost:" + port + "/api/webhook/lemonsqueezy");
        LOGGER.info("   - POST http://localhost:" + port + "/api/webhook/stripe");
        LOGGER.info("   - GET  http://localhost:" + port + "/api/license/verify?key=sec_live_...");
        LOGGER.info("   - GET  http://localhost:" + port + "/api/licenses");
        LOGGER.info("   - GET  http://localhost:" + port + "/api/health");
        LOGGER.info("================================================================================");
    }

    public void stop() {
        server.stop(0);
        LOGGER.info("Billing server stopped.");
    }

    // -------------------------------- Handlers --------------------------------

    private void handleCreateCheckoutSession(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed. Use POST."));
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            CheckoutRequest req = body.isBlank() ? new CheckoutRequest() : objectMapper.readValue(body, CheckoutRequest.class);
            CheckoutResponse resp = "lemonsqueezy".equalsIgnoreCase(activeProvider)
                    ? lemonSqueezyService.createCheckoutSession(req)
                    : stripeService.createCheckoutSession(req);
            sendJsonResponse(exchange, 200, resp);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating checkout session", e);
            sendJsonResponse(exchange, 400, CheckoutResponse.error("Failed to create checkout session: " + e.getMessage()));
        }
    }

    private void handleLemonSqueezyWebhook(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed. Use POST."));
            return;
        }

        try {
            String payload = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String sigHeader = exchange.getRequestHeaders().getFirst("X-Signature");

            boolean validSig = lemonSqueezyService.verifyWebhookSignature(payload, sigHeader);
            if (!validSig) {
                LOGGER.warning("Invalid Lemon Squeezy webhook signature rejected.");
                sendJsonResponse(exchange, 400, Map.of("error", "Invalid webhook signature"));
                return;
            }

            Map<String, Object> result = lemonSqueezyService.processWebhookEvent(payload);
            sendJsonResponse(exchange, 200, result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing Lemon Squeezy webhook", e);
            sendJsonResponse(exchange, 400, Map.of("error", e.getMessage()));
        }
    }

    private void handleStripeWebhook(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed. Use POST."));
            return;
        }

        try {
            String payload = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String sigHeader = exchange.getRequestHeaders().getFirst("Stripe-Signature");

            boolean validSig = stripeService.verifyWebhookSignature(payload, sigHeader);
            if (!validSig) {
                LOGGER.warning("Invalid Stripe webhook signature rejected.");
                sendJsonResponse(exchange, 400, Map.of("error", "Invalid webhook signature"));
                return;
            }

            Map<String, Object> result = stripeService.processWebhookEvent(payload);
            sendJsonResponse(exchange, 200, result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing Stripe webhook", e);
            sendJsonResponse(exchange, 400, Map.of("error", e.getMessage()));
        }
    }

    private void handleLicenseVerify(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed. Use GET."));
            return;
        }

        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();
        String key = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && "key".equalsIgnoreCase(pair[0])) {
                    key = pair[1];
                    break;
                }
            }
        }

        LicenseVerifyResponse response = licenseManager.verifyLicense(key);
        int statusCode = response.isValid() ? 200 : 403;
        sendJsonResponse(exchange, statusCode, response);
    }

    private void handleListLicenses(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        sendJsonResponse(exchange, 200, licenseManager.getAllLicenses());
    }

    private void handleHealthCheck(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("service", "API Security Scanner Billing & License API");
        health.put("activeProvider", activeProvider);
        health.put("lemonSqueezyLiveConfigured", lemonSqueezyService.isLiveConfigured());
        health.put("stripeLiveConfigured", stripeService.isStripeLiveConfigured());
        health.put("totalLicensesIssued", licenseManager.getAllLicenses().size());
        health.put("timestamp", Instant.now().toString());

        sendJsonResponse(exchange, 200, health);
    }

    // -------------------------------- Utility Methods --------------------------------

    private void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization, Stripe-Signature, X-Signature, X-Requested-With");
        headers.set("Access-Control-Max-Age", "86400");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String resolveConfig(Properties props, String key, String envVar, String defaultVal) {
        String env = System.getenv(envVar);
        if (env != null && !env.isBlank()) {
            return env;
        }
        if (props != null && props.containsKey(key)) {
            String val = props.getProperty(key);
            if (val != null && !val.isBlank()) return val;
        }
        return defaultVal;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        // Check current directory application.properties first
        File localFile = new File("application.properties");
        if (localFile.exists()) {
            try (InputStream is = new FileInputStream(localFile)) {
                props.load(is);
                return props;
            } catch (IOException ignored) {}
        }
        // Fallback to classpath
        try (InputStream is = BillingServer.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {}
        return props;
    }

    public static void main(String[] args) {
        try {
            Properties props = loadProperties();
            int port = DEFAULT_PORT;
            String portStr = System.getenv("PORT");
            if (portStr == null || portStr.isBlank()) {
                portStr = props.getProperty("server.port");
            }
            if (portStr != null && !portStr.isBlank()) {
                try {
                    port = Integer.parseInt(portStr.trim());
                } catch (NumberFormatException ignored) {}
            }

            BillingServer server = new BillingServer(port, props);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down billing server...");
                server.stop();
            }));

            // Keep main thread alive
            Thread.currentThread().join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start BillingServer", e);
            System.exit(1);
        }
    }
}
