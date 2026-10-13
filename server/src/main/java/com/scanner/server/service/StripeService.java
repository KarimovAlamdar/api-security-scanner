package com.scanner.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.server.models.CheckoutRequest;
import com.scanner.server.models.CheckoutResponse;
import com.scanner.server.models.LicenseInfo;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles communication with the Stripe API (Checkout Sessions & Webhook signatures).
 */
public class StripeService {
    private static final Logger LOGGER = Logger.getLogger(StripeService.class.getName());
    private static final String STRIPE_API_URL = "https://api.stripe.com/v1/checkout/sessions";

    private final String stripeSecretKey;
    private final String webhookSecret;
    private final String frontendUrl;
    private final LicenseManager licenseManager;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Base pricing configuration (in USD cents)
    // Starter: $19/mo or $15/mo billed annually ($180/yr)
    // Pro: $49/mo or $39/mo billed annually ($468/yr)
    // Enterprise: $299/mo or $239/mo billed annually ($2868/yr)
    private static final Map<String, Long> MONTHLY_PRICES = Map.of(
            "COMMUNITY", 0L,
            "STARTER", 1900L,
            "PRO", 4900L,
            "ENTERPRISE", 29900L
    );

    private static final Map<String, Long> ANNUAL_PRICES = Map.of(
            "COMMUNITY", 0L,
            "STARTER", 18000L,   // $15 * 12 = $180
            "PRO", 46800L,       // $39 * 12 = $468
            "ENTERPRISE", 286800L // $239 * 12 = $2868
    );

    public StripeService(String stripeSecretKey, String webhookSecret, String frontendUrl, LicenseManager licenseManager) {
        this.stripeSecretKey = (stripeSecretKey != null) ? stripeSecretKey.trim() : "";
        this.webhookSecret = (webhookSecret != null) ? webhookSecret.trim() : "";
        this.frontendUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl.trim() : "http://localhost:3000";
        this.licenseManager = licenseManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks if real Stripe API credentials are configured.
     */
    public boolean isStripeLiveConfigured() {
        return stripeSecretKey.startsWith("sk_") && !stripeSecretKey.contains("placeholder");
    }

    /**
     * Creates a checkout session. If real Stripe keys are configured, calls Stripe REST API.
     * Otherwise runs in safe sandbox simulation mode.
     */
    public CheckoutResponse createCheckoutSession(CheckoutRequest req) {
        String tier = (req.getTier() == null) ? "Pro" : req.getTier().trim();
        String tierUpper = tier.toUpperCase(Locale.ROOT);
        String cycle = (req.getBillingCycle() == null || req.getBillingCycle().isBlank()) ? "monthly" : req.getBillingCycle().toLowerCase(Locale.ROOT);
        boolean isAnnual = "annual".equalsIgnoreCase(cycle);

        long baseAmount = isAnnual 
                ? ANNUAL_PRICES.getOrDefault(tierUpper, 46800L) 
                : MONTHLY_PRICES.getOrDefault(tierUpper, 4900L);

        // Check Early Bird 50% coupon code
        boolean discountApplied = false;
        String coupon = req.getCouponCode();
        long finalAmount = baseAmount;
        if (coupon != null && coupon.trim().equalsIgnoreCase("EARLYBIRD10")) {
            finalAmount = Math.round(baseAmount * 0.5);
            discountApplied = true;
        }

        // Community free tier
        if ("COMMUNITY".equals(tierUpper)) {
            String pseudoSession = "cs_free_" + UUID.randomUUID().toString().substring(0, 12);
            LicenseInfo freeLicense = licenseManager.issueLicense(
                    req.getCustomerEmail(), "Community", cycle, pseudoSession, "cus_free"
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

        // Real Stripe API call if secret key is present
        if (isStripeLiveConfigured()) {
            try {
                return callStripeApi(req, tier, cycle, finalAmount, discountApplied);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Real Stripe API call failed: " + e.getMessage() + ". Falling back to sandbox simulation.", e);
            }
        }

        // Sandbox / Test Simulation fallback
        String mockSessionId = "cs_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        LicenseInfo simulatedLicense = licenseManager.issueLicense(
                req.getCustomerEmail(), tier, cycle, mockSessionId, "cus_mock_" + UUID.randomUUID().toString().substring(0, 8)
        );

        String successRedirect = buildRedirectUrl(req.getSuccessUrl(), mockSessionId, simulatedLicense.getLicenseKey());

        CheckoutResponse resp = CheckoutResponse.success(
                mockSessionId,
                successRedirect,
                tier,
                cycle,
                finalAmount,
                discountApplied,
                "sandbox_simulation",
                "Simulated Stripe Checkout session created. Configure STRIPE_SECRET_KEY in application.properties for live checkout."
        );
        resp.setSimulatedLicenseKey(simulatedLicense.getLicenseKey());
        if (discountApplied) {
            resp.setDiscountDetails("50% Early Bird Discount applied (Coupon: EARLYBIRD10)");
        }
        return resp;
    }

    private CheckoutResponse callStripeApi(CheckoutRequest req, String tier, String cycle, long amountCents, boolean discountApplied) throws Exception {
        String successUrl = (req.getSuccessUrl() != null && !req.getSuccessUrl().isBlank())
                ? req.getSuccessUrl()
                : frontendUrl + "/index.html?session_id={CHECKOUT_SESSION_ID}&status=success";
        String cancelUrl = (req.getCancelUrl() != null && !req.getCancelUrl().isBlank())
                ? req.getCancelUrl()
                : frontendUrl + "/index.html?status=cancelled";

        String productName = "API Security Scanner (" + tier + " Plan - " + (cycle.equalsIgnoreCase("annual") ? "Annual" : "Monthly") + ")";

        // Build URL-encoded form parameters for Stripe API
        Map<String, String> params = new LinkedHashMap<>();
        params.put("success_url", successUrl);
        params.put("cancel_url", cancelUrl);
        params.put("mode", "payment"); // Or "subscription" if recurring prices are provisioned in Stripe Dashboard
        params.put("payment_method_types[0]", "card");
        params.put("line_items[0][price_data][currency]", "usd");
        params.put("line_items[0][price_data][product_data][name]", productName);
        params.put("line_items[0][price_data][product_data][description]", "Automated DAST + SAST Hybrid Vulnerability Scanner License");
        params.put("line_items[0][price_data][unit_amount]", String.valueOf(amountCents));
        params.put("line_items[0][quantity]", "1");
        
        if (req.getCustomerEmail() != null && !req.getCustomerEmail().isBlank()) {
            params.put("customer_email", req.getCustomerEmail().trim());
        }

        params.put("metadata[tier]", tier);
        params.put("metadata[billingCycle]", cycle);
        params.put("metadata[discountApplied]", String.valueOf(discountApplied));
        if (req.getCouponCode() != null) {
            params.put("metadata[couponCode]", req.getCouponCode());
        }

        String formData = encodeFormData(params);

        HttpRequest stripeReq = HttpRequest.newBuilder()
                .uri(URI.create(STRIPE_API_URL))
                .header("Authorization", "Bearer " + stripeSecretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "ApiSecurityScanner-Billing/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(formData, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(stripeReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            String sessionId = root.path("id").asText();
            String checkoutUrl = root.path("url").asText();

            CheckoutResponse okResp = CheckoutResponse.success(
                    sessionId,
                    checkoutUrl,
                    tier,
                    cycle,
                    amountCents,
                    discountApplied,
                    "stripe_live",
                    "Live Stripe Checkout Session created successfully."
            );
            if (discountApplied) {
                okResp.setDiscountDetails("50% Early Bird Discount applied (Coupon: EARLYBIRD10)");
            }
            return okResp;
        } else {
            LOGGER.severe("Stripe API error (" + response.statusCode() + "): " + response.body());
            throw new RuntimeException("Stripe responded with code " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Verifies Stripe Webhook signature header (`Stripe-Signature`).
     */
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (webhookSecret.isEmpty() || webhookSecret.contains("placeholder")) {
            // In development or if secret is not set, allow processing
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        try {
            long timestamp = -1;
            String signature = null;

            for (String part : signatureHeader.split(",")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2) {
                    if ("t".equalsIgnoreCase(kv[0])) {
                        timestamp = Long.parseLong(kv[1]);
                    } else if ("v1".equalsIgnoreCase(kv[0])) {
                        signature = kv[1];
                    }
                }
            }

            if (timestamp == -1 || signature == null) {
                return false;
            }

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String expected = sb.toString();

            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Webhook signature verification error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Processes inbound Stripe webhook events (e.g. checkout.session.completed).
     */
    public Map<String, Object> processWebhookEvent(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        String eventType = root.path("type").asText();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("received", true);
        result.put("eventType", eventType);

        if ("checkout.session.completed".equalsIgnoreCase(eventType)) {
            JsonNode sessionNode = root.path("data").path("object");
            String sessionId = sessionNode.path("id").asText();
            String customerEmail = sessionNode.path("customer_details").path("email").asText();
            if (customerEmail == null || customerEmail.isBlank()) {
                customerEmail = sessionNode.path("customer_email").asText("customer@example.com");
            }
            String customerId = sessionNode.path("customer").asText("cus_none");

            JsonNode metadata = sessionNode.path("metadata");
            String tier = metadata.path("tier").asText("Pro");
            String cycle = metadata.path("billingCycle").asText("monthly");

            LicenseInfo license = licenseManager.issueLicense(customerEmail, tier, cycle, sessionId, customerId);
            result.put("licenseKey", license.getLicenseKey());
            result.put("tier", license.getTier());
            result.put("customerEmail", license.getCustomerEmail());
            LOGGER.info(() -> "Successfully provisioned license " + license.getLicenseKey() + " for Stripe session " + sessionId);
        }

        return result;
    }

    private String buildRedirectUrl(String customUrl, String sessionId, String licenseKey) {
        if (customUrl != null && !customUrl.isBlank()) {
            return customUrl.replace("{CHECKOUT_SESSION_ID}", sessionId) + (customUrl.contains("?") ? "&" : "?") + "license=" + licenseKey;
        }
        return frontendUrl + "/index.html?session_id=" + sessionId + "&status=success&license=" + licenseKey;
    }

    private String encodeFormData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append("=")
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
