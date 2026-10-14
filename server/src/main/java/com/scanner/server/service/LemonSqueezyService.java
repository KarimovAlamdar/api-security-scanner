package com.scanner.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.server.models.CheckoutRequest;
import com.scanner.server.models.CheckoutResponse;
import com.scanner.server.models.LicenseInfo;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for Lemon Squeezy Checkout API and Webhook verification (Merchant of Record).
 * Handles global payments, tax compliance, and direct payouts to Wise, Payoneer, or local bank.
 */
public class LemonSqueezyService {
    private static final Logger LOGGER = Logger.getLogger(LemonSqueezyService.class.getName());
    private static final String LEMON_API_URL = "https://api.lemonsqueezy.com/v1/checkouts";

    private final String apiKey;
    private final String storeId;
    private final String webhookSecret;
    private final Map<String, String> variantMap;
    private final String frontendUrl;
    private final LicenseManager licenseManager;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LemonSqueezyService(String apiKey, String storeId, String webhookSecret, 
                               Map<String, String> variantMap, String frontendUrl, 
                               LicenseManager licenseManager) {
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
        this.storeId = (storeId != null) ? storeId.trim() : "";
        this.webhookSecret = (webhookSecret != null) ? webhookSecret.trim() : "";
        this.variantMap = (variantMap != null) ? variantMap : Collections.emptyMap();
        this.frontendUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl.trim() : "http://localhost:3000";
        this.licenseManager = licenseManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isLiveConfigured() {
        return !apiKey.isBlank() && !apiKey.contains("placeholder") 
                && !storeId.isBlank() && !storeId.contains("placeholder");
    }

    /**
     * Creates a Lemon Squeezy Checkout session.
     */
    public CheckoutResponse createCheckoutSession(CheckoutRequest req) {
        String tier = (req.getTier() == null) ? "Pro" : req.getTier().trim();
        String cycle = (req.getBillingCycle() == null || req.getBillingCycle().isBlank()) ? "monthly" : req.getBillingCycle().toLowerCase(Locale.ROOT);
        boolean isAnnual = "annual".equalsIgnoreCase(cycle);

        // Community Free Tier
        if ("Community".equalsIgnoreCase(tier)) {
            String pseudoSession = "ls_free_" + UUID.randomUUID().toString().substring(0, 10);
            LicenseInfo freeLicense = licenseManager.issueLicense(
                    req.getCustomerEmail(), "Community", cycle, pseudoSession, "ls_free_user"
            );
            CheckoutResponse resp = CheckoutResponse.success(
                    pseudoSession,
                    buildRedirectUrl(req.getSuccessUrl(), pseudoSession, freeLicense.getLicenseKey()),
                    "Community",
                    cycle,
                    0,
                    false,
                    "free_tier",
                    "Community tier activated successfully."
            );
            resp.setSimulatedLicenseKey(freeLicense.getLicenseKey());
            return resp;
        }

        boolean discountApplied = false;
        String coupon = req.getCouponCode();
        if (coupon != null && coupon.trim().equalsIgnoreCase("EARLYBIRD10")) {
            discountApplied = true;
        }

        // Live Lemon Squeezy API Call
        if (isLiveConfigured()) {
            try {
                return callLemonSqueezyApi(req, tier, cycle, discountApplied);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Lemon Squeezy API call failed: " + e.getMessage() + ". Falling back to sandbox simulation.", e);
            }
        }

        // Sandbox / Test Simulation fallback
        String mockOrderId = "ls_order_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        LicenseInfo simulatedLicense = licenseManager.issueLicense(
                req.getCustomerEmail(), tier, cycle, mockOrderId, "ls_cust_" + UUID.randomUUID().toString().substring(0, 8)
        );

        String successRedirect = buildRedirectUrl(req.getSuccessUrl(), mockOrderId, simulatedLicense.getLicenseKey());

        long estimatedAmount = calculateEstimatedAmount(tier, isAnnual, discountApplied);

        CheckoutResponse resp = CheckoutResponse.success(
                mockOrderId,
                successRedirect,
                tier,
                cycle,
                estimatedAmount,
                discountApplied,
                "sandbox_simulation",
                "Lemon Squeezy Checkout simulation active. Payouts route directly to Payoneer/Wise/Bank once API keys are added."
        );
        resp.setSimulatedLicenseKey(simulatedLicense.getLicenseKey());
        if (discountApplied) {
            resp.setDiscountDetails("50% Early Bird Discount applied (Coupon: EARLYBIRD10)");
        }
        return resp;
    }

    private CheckoutResponse callLemonSqueezyApi(CheckoutRequest req, String tier, String cycle, boolean discountApplied) throws Exception {
        String variantId = variantMap.getOrDefault(tier.toLowerCase(Locale.ROOT), "");
        if (variantId.isBlank() || variantId.contains("placeholder")) {
            throw new IllegalStateException("Variant ID not configured for tier: " + tier);
        }

        String successUrl = (req.getSuccessUrl() != null && !req.getSuccessUrl().isBlank())
                ? req.getSuccessUrl()
                : frontendUrl + "/index.html?session_id={CHECKOUT_SESSION_ID}&status=success";

        // Build Lemon Squeezy JSON:API Payload
        Map<String, Object> customData = new HashMap<>();
        customData.put("tier", tier);
        customData.put("billing_cycle", cycle);

        Map<String, Object> checkoutData = new HashMap<>();
        if (req.getCustomerEmail() != null && !req.getCustomerEmail().isBlank()) {
            checkoutData.put("email", req.getCustomerEmail().trim());
        }
        if (discountApplied) {
            checkoutData.put("discount_code", "EARLYBIRD10");
        }
        checkoutData.put("custom", customData);

        Map<String, Object> productOptions = new HashMap<>();
        productOptions.put("redirect_url", successUrl);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("checkout_data", checkoutData);
        attributes.put("product_options", productOptions);

        Map<String, Object> storeRel = Map.of("data", Map.of("type", "stores", "id", storeId));
        Map<String, Object> variantRel = Map.of("data", Map.of("type", "variants", "id", variantId));
        Map<String, Object> relationships = Map.of("store", storeRel, "variant", variantRel);

        Map<String, Object> dataObj = new HashMap<>();
        dataObj.put("type", "checkouts");
        dataObj.put("attributes", attributes);
        dataObj.put("relationships", relationships);

        Map<String, Object> rootPayload = Map.of("data", dataObj);
        String jsonBody = objectMapper.writeValueAsString(rootPayload);

        HttpRequest lemonReq = HttpRequest.newBuilder()
                .uri(URI.create(LEMON_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/vnd.api+json")
                .header("Accept", "application/vnd.api+json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(lemonReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            String checkoutId = root.path("data").path("id").asText();
            String checkoutUrl = root.path("data").path("attributes").path("url").asText();

            long estimatedAmount = calculateEstimatedAmount(tier, "annual".equalsIgnoreCase(cycle), discountApplied);

            CheckoutResponse okResp = CheckoutResponse.success(
                    checkoutId,
                    checkoutUrl,
                    tier,
                    cycle,
                    estimatedAmount,
                    discountApplied,
                    "lemonsqueezy_live",
                    "Live Lemon Squeezy Checkout created successfully."
            );
            if (discountApplied) {
                okResp.setDiscountDetails("50% Early Bird Discount applied (Coupon: EARLYBIRD10)");
            }
            return okResp;
        } else {
            LOGGER.severe("Lemon Squeezy API error (" + response.statusCode() + "): " + response.body());
            throw new RuntimeException("Lemon Squeezy responded with code " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Verifies Lemon Squeezy webhook signature header (`X-Signature`).
     */
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (webhookSecret.isEmpty() || webhookSecret.contains("placeholder")) {
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String expected = sb.toString();

            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signatureHeader.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Lemon Squeezy signature verification error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Processes inbound Lemon Squeezy webhook events (order_created, subscription_created).
     */
    public Map<String, Object> processWebhookEvent(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        String eventName = root.path("meta").path("event_name").asText();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("received", true);
        result.put("eventName", eventName);

        if ("order_created".equalsIgnoreCase(eventName) || "subscription_created".equalsIgnoreCase(eventName)) {
            JsonNode dataNode = root.path("data");
            String orderId = dataNode.path("id").asText();
            JsonNode attr = dataNode.path("attributes");
            String customerEmail = attr.path("user_email").asText("customer@example.com");
            String customerId = String.valueOf(attr.path("customer_id").asLong());

            // Extract custom tier or infer from first order item
            JsonNode custom = root.path("meta").path("custom_data");
            String tier = custom.path("tier").asText("");
            String cycle = custom.path("billing_cycle").asText("monthly");

            if (tier.isBlank()) {
                String productName = attr.path("first_order_item").path("product_name").asText();
                if (productName.toLowerCase().contains("starter")) tier = "Starter";
                else if (productName.toLowerCase().contains("enterprise")) tier = "Enterprise";
                else tier = "Pro";
                if (productName.toLowerCase().contains("anual") || productName.toLowerCase().contains("annual") || productName.toLowerCase().contains("year")) {
                    cycle = "annual";
                }
            }

            LicenseInfo license = licenseManager.issueLicense(customerEmail, tier, cycle, orderId, "ls_cust_" + customerId);
            result.put("licenseKey", license.getLicenseKey());
            result.put("tier", license.getTier());
            result.put("customerEmail", license.getCustomerEmail());
            LOGGER.info(() -> "Lemon Squeezy Webhook: Issued " + license.getTier() + " license " + license.getLicenseKey() + " to " + customerEmail);
        }

        return result;
    }

    private long calculateEstimatedAmount(String tier, boolean isAnnual, boolean discountApplied) {
        long base;
        switch (tier.toUpperCase(Locale.ROOT)) {
            case "STARTER" -> base = isAnnual ? 18000L : 1900L;
            case "ENTERPRISE" -> base = isAnnual ? 286800L : 29900L;
            default -> base = isAnnual ? 46800L : 4900L; // Pro
        }
        return discountApplied ? Math.round(base * 0.5) : base;
    }

    private String buildRedirectUrl(String customUrl, String sessionId, String licenseKey) {
        if (customUrl != null && !customUrl.isBlank()) {
            return customUrl.replace("{CHECKOUT_SESSION_ID}", sessionId) + (customUrl.contains("?") ? "&" : "?") + "license=" + licenseKey;
        }
        return frontendUrl + "/index.html?session_id=" + sessionId + "&status=success&license=" + licenseKey;
    }
}
