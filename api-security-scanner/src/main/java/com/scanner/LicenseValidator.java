package com.scanner;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates scanner customer license keys against the billing server
 * and enforces plan tier restrictions (Community, Starter, Pro, Enterprise).
 */
public class LicenseValidator {

    private static final String DEFAULT_HMAC_SECRET = "api-security-scanner-secret-hmac-key-2026";
    private static final Duration NETWORK_TIMEOUT = Duration.ofSeconds(4);

    public record LicenseStatus(
            boolean valid,
            String tier,
            String customerEmail,
            int maxRepos,
            Set<String> features,
            String message,
            boolean isOffline
    ) {
        public boolean isFeatureAllowed(String feature) {
            return features.contains(feature);
        }
    }

    /**
     * Validates the license key against the remote license server or offline HMAC fallback.
     */
    public static LicenseStatus validate(String licenseKey, String serverUrl, boolean airGapped) {
        // 1. Community Free Tier (No key provided)
        if (licenseKey == null || licenseKey.isBlank()) {
            return new LicenseStatus(
                    true,
                    "Community",
                    "community@open-source.org",
                    1,
                    Set.of("DAST_CORE", "SARIF_EXPORT"),
                    "Running in Community Free Edition (Core DAST & SARIF export).",
                    true
            );
        }

        String key = licenseKey.trim();

        // 2. Format validation: must start with sec_live_
        if (!key.startsWith("sec_live_")) {
            return new LicenseStatus(
                    false,
                    "Unknown",
                    null,
                    0,
                    Collections.emptySet(),
                    "Invalid license key format: Must start with 'sec_live_'",
                    true
            );
        }

        // Community free key generated from portal
        if (key.startsWith("sec_live_free_") || key.contains("community")) {
            return new LicenseStatus(
                    true,
                    "Community",
                    "community@open-source.org",
                    1,
                    Set.of("DAST_CORE", "SARIF_EXPORT"),
                    "Running in Community Free Edition (Core DAST & SARIF export).",
                    true
            );
        }

        // 3. Air-Gapped Mode: Perform offline cryptographic validation
        if (airGapped) {
            if (verifyOfflineHmac(key, DEFAULT_HMAC_SECRET)) {
                String tier = resolveTierFromKey(key);
                int maxRepos = "Enterprise".equalsIgnoreCase(tier) ? 9999 : ("Starter".equalsIgnoreCase(tier) ? 3 : 15);
                return new LicenseStatus(
                        true,
                        tier,
                        "airgap@enterprise.internal",
                        maxRepos,
                        resolveFeatures(tier),
                        "Air-Gapped Mode: Offline cryptographic license signature verified (" + tier + " Plan).",
                        true
                );
            } else {
                return new LicenseStatus(
                        false,
                        "Unknown",
                        null,
                        0,
                        Collections.emptySet(),
                        "Air-Gapped license verification failed: Checksum mismatch.",
                        true
                );
            }
        }

        // 4. Remote Server Verification
        String baseUrl = (serverUrl == null || serverUrl.isBlank()) ? "http://localhost:4242" : serverUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String verifyEndpoint = baseUrl + "/api/license/verify?key=" + key;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(NETWORK_TIMEOUT)
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(verifyEndpoint))
                    .header("Accept", "application/json")
                    .timeout(NETWORK_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() == 200) {
                return parseJsonResponse(resp.body(), key);
            } else if (resp.statusCode() == 403) {
                String errorMsg = extractJsonField(resp.body(), "message");
                return new LicenseStatus(
                        false,
                        "Invalid",
                        null,
                        0,
                        Collections.emptySet(),
                        errorMsg != null ? errorMsg : "License key is inactive or expired.",
                        false
                );
            }
        } catch (Exception e) {
            // Server offline or network timeout: Fallback gracefully to offline HMAC verification
            if (verifyOfflineHmac(key, DEFAULT_HMAC_SECRET)) {
                String tier = resolveTierFromKey(key);
                int maxRepos = "Enterprise".equalsIgnoreCase(tier) ? 9999 : ("Starter".equalsIgnoreCase(tier) ? 3 : 15);
                return new LicenseStatus(
                        true,
                        tier,
                        "offline-cached@customer.org",
                        maxRepos,
                        resolveFeatures(tier),
                        "License verified via offline cryptographic backup (Server unreachable: " + e.getMessage() + ").",
                        true
                );
            }
        }

        // Fallback invalid
        return new LicenseStatus(
                false,
                "Unknown",
                null,
                0,
                Collections.emptySet(),
                "License verification failed or server unreachable.",
                false
        );
    }

    /**
     * Enforces feature entitlements according to the license tier.
     */
    public static void enforceFeatureGate(LicenseStatus status, boolean autoPr, boolean hasDualRole) {
        if (!status.valid()) {
            throw new IllegalArgumentException("[LICENSE ERROR] " + status.message());
        }

        // Print welcome badge
        System.out.println("================================================================================");
        System.out.printf("🔑 API Security Scanner License: [%s Plan] - %s%n", status.tier().toUpperCase(Locale.ROOT), status.message());
        if (!"Community".equalsIgnoreCase(status.tier())) {
            System.out.printf("   Customer: %s | Max Repositories: %d | Features: %s%n",
                    status.customerEmail(), status.maxRepos(), status.features());
        } else {
            System.out.println("   💡 Upgrade to Pro / Enterprise for Zero-Click AI PR Auto-Fix, Stateful BOLA & Shadow API discovery.");
        }
        System.out.println("================================================================================");

        // Enforce Auto-PR requirement
        if (autoPr && !status.isFeatureAllowed("AUTO_PR_BOT")) {
            throw new IllegalArgumentException(String.format(
                    "[LICENSE RESTRICTION] The '--auto-pr' (Zero-Click AI PR Auto-Fix) feature requires a Pro or Enterprise plan.%n" +
                    "Current active plan: %s. Upgrade your license at https://scanner.dev/pricing",
                    status.tier()
            ));
        }

        // Dual-role BOLA notice
        if (hasDualRole && !status.isFeatureAllowed("STATEFUL_BOLA")) {
            System.out.println("[LICENSE NOTICE] Advanced Stateful Cross-Tenant BOLA analysis is optimized for Pro/Enterprise tiers.");
        }
    }

    private static boolean verifyOfflineHmac(String key, String secret) {
        if (!key.startsWith("sec_live_")) return false;
        String raw = key.substring("sec_live_".length());
        String[] parts = raw.split("_");
        if (parts.length != 2) return false;
        String token = parts[0];
        String signature = parts[1];

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String expected = sb.substring(0, 10);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private static LicenseStatus parseJsonResponse(String json, String key) {
        String tier = extractJsonField(json, "tier");
        if (tier == null) tier = "Pro";
        String email = extractJsonField(json, "customerEmail");
        if (email == null) email = "customer@company.com";
        String msg = extractJsonField(json, "message");
        if (msg == null) msg = "License is active and valid.";

        int maxRepos = "Enterprise".equalsIgnoreCase(tier) ? 9999 : ("Starter".equalsIgnoreCase(tier) ? 3 : 15);
        Set<String> features = resolveFeatures(tier);

        return new LicenseStatus(true, tier, email, maxRepos, features, msg, false);
    }

    private static Set<String> resolveFeatures(String tier) {
        if ("Community".equalsIgnoreCase(tier)) {
            return Set.of("DAST_CORE", "SARIF_EXPORT");
        }
        if ("Starter".equalsIgnoreCase(tier)) {
            return Set.of("DAST_CORE", "SAST_MAPPING", "SARIF_EXPORT", "HTML_REPORT");
        }
        if ("Enterprise".equalsIgnoreCase(tier)) {
            return Set.of("DAST_CORE", "SAST_MAPPING", "AUTO_PR_BOT", "SHADOW_API_DISCOVERY",
                    "STATEFUL_BOLA", "SARIF_EXPORT", "HTML_REPORT", "CUSTOM_RULES", "PRIORITY_SLA");
        }
        // Default: Pro
        return Set.of("DAST_CORE", "SAST_MAPPING", "AUTO_PR_BOT", "SHADOW_API_DISCOVERY",
                "STATEFUL_BOLA", "SARIF_EXPORT", "HTML_REPORT");
    }

    private static String extractJsonField(String json, String field) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String resolveTierFromKey(String key) {
        if (key == null) return "Pro";
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.startsWith("sec_live_ent") || lower.contains("enterprise")) return "Enterprise";
        if (lower.startsWith("sec_live_sta") || lower.contains("starter")) return "Starter";
        return "Pro";
    }
}
