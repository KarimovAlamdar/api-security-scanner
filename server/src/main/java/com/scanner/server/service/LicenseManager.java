package com.scanner.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scanner.server.models.LicenseInfo;
import com.scanner.server.models.LicenseVerifyResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages issuing, persistent storage, and verification of customer license keys.
 */
public class LicenseManager {
    private static final Logger LOGGER = Logger.getLogger(LicenseManager.class.getName());
    private static final String DEFAULT_STORAGE_PATH = "data/licenses.json";

    private final ObjectMapper objectMapper;
    private final File storageFile;
    private final String hmacSecret;
    private final Map<String, LicenseInfo> licenses = new ConcurrentHashMap<>();

    public LicenseManager() {
        this(DEFAULT_STORAGE_PATH, "api-security-scanner-secret-hmac-key-2026");
    }

    public LicenseManager(String storageFilePath, String hmacSecret) {
        this.storageFile = new File(storageFilePath);
        this.hmacSecret = hmacSecret;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        loadLicensesFromDisk();
    }

    /**
     * Issues a new license after successful Stripe checkout.
     */
    public synchronized LicenseInfo issueLicense(String customerEmail, String tier, String billingCycle, 
                                                 String stripeSessionId, String stripeCustomerId) {
        String cleanTier = (tier == null || tier.isBlank()) ? "Pro" : tier.trim();
        String cleanCycle = (billingCycle == null || billingCycle.isBlank()) ? "monthly" : billingCycle.trim();
        String email = (customerEmail == null || customerEmail.isBlank()) ? "user@example.com" : customerEmail.trim();

        Instant now = Instant.now();
        Instant expiresAt = cleanCycle.equalsIgnoreCase("annual") 
                ? now.plus(365, ChronoUnit.DAYS) 
                : now.plus(30, ChronoUnit.DAYS);

        int maxRepos = resolveMaxRepos(cleanTier);
        List<String> features = resolveFeatures(cleanTier);

        String tierPrefix = cleanTier.toLowerCase(Locale.ROOT).substring(0, Math.min(3, cleanTier.length()));
        String rawToken = tierPrefix + (UUID.randomUUID().toString().replace("-", "") + Long.toHexString(now.toEpochMilli()));
        String token = rawToken.substring(0, 24);
        String checksum = computeHmac(token, hmacSecret).substring(0, 10);
        String licenseKey = "sec_live_" + token + "_" + checksum;

        LicenseInfo license = new LicenseInfo(
                licenseKey,
                email,
                cleanTier,
                cleanCycle,
                "ACTIVE",
                maxRepos,
                features,
                now.toString(),
                expiresAt.toString(),
                stripeSessionId,
                stripeCustomerId
        );

        licenses.put(licenseKey, license);
        saveLicensesToDisk();
        LOGGER.info(() -> String.format("Issued %s license for %s: %s", cleanTier, email, licenseKey));
        return license;
    }

    /**
     * Verifies whether a license key is valid, unexpired, and active.
     */
    public LicenseVerifyResponse verifyLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return LicenseVerifyResponse.invalid("License key is required");
        }

        String key = licenseKey.trim();
        LicenseInfo license = licenses.get(key);

        if (license == null) {
            // Also allow offline HMAC validation for air-gapped enterprise users
            if (isValidOfflineKey(key)) {
                LicenseInfo simulated = new LicenseInfo(
                        key,
                        "offline-enterprise@airgap.internal",
                        "Enterprise",
                        "annual",
                        "ACTIVE",
                        9999,
                        resolveFeatures("Enterprise"),
                        Instant.now().toString(),
                        Instant.now().plus(365, ChronoUnit.DAYS).toString(),
                        "offline",
                        "offline"
                );
                return LicenseVerifyResponse.valid(simulated);
            }
            return LicenseVerifyResponse.invalid("License key not found or unrecognized");
        }

        if (!"ACTIVE".equalsIgnoreCase(license.getStatus())) {
            return LicenseVerifyResponse.invalid("License status is " + license.getStatus());
        }

        if (license.getExpiresAt() != null) {
            Instant expiry = Instant.parse(license.getExpiresAt());
            if (Instant.now().isAfter(expiry)) {
                license.setStatus("EXPIRED");
                saveLicensesToDisk();
                return LicenseVerifyResponse.invalid("License expired on " + license.getExpiresAt());
            }
        }

        return LicenseVerifyResponse.valid(license);
    }

    public List<LicenseInfo> getAllLicenses() {
        return new ArrayList<>(licenses.values());
    }

    public Optional<LicenseInfo> getLicenseByKey(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(licenses.get(key.trim()));
    }

    public Optional<LicenseInfo> getLicenseBySessionId(String sessionId) {
        if (sessionId == null) return Optional.empty();
        return licenses.values().stream()
                .filter(l -> sessionId.equals(l.getStripeSessionId()))
                .findFirst();
    }

    private int resolveMaxRepos(String tier) {
        if ("Community".equalsIgnoreCase(tier)) return 1;
        if ("Starter".equalsIgnoreCase(tier)) return 3;
        if ("Enterprise".equalsIgnoreCase(tier)) return 9999;
        return 15; // Pro
    }

    private List<String> resolveFeatures(String tier) {
        if ("Community".equalsIgnoreCase(tier)) {
            return List.of("DAST_CORE", "SARIF_EXPORT");
        }
        if ("Starter".equalsIgnoreCase(tier)) {
            return List.of("DAST_CORE", "SAST_MAPPING", "SARIF_EXPORT", "HTML_REPORT");
        }
        if ("Enterprise".equalsIgnoreCase(tier)) {
            return List.of("DAST_CORE", "SAST_MAPPING", "AUTO_PR_BOT", "SHADOW_API_DISCOVERY", 
                           "STATEFUL_BOLA", "SARIF_EXPORT", "HTML_REPORT", "CUSTOM_RULES", "PRIORITY_SLA");
        }
        // Default: Pro
        return List.of("DAST_CORE", "SAST_MAPPING", "AUTO_PR_BOT", "SHADOW_API_DISCOVERY", 
                       "STATEFUL_BOLA", "SARIF_EXPORT", "HTML_REPORT");
    }

    private boolean isValidOfflineKey(String key) {
        if (!key.startsWith("sec_live_")) return false;
        String[] parts = key.substring("sec_live_".length()).split("_");
        if (parts.length != 2) return false;
        String token = parts[0];
        String expectedChecksum = computeHmac(token, hmacSecret).substring(0, 10);
        return expectedChecksum.equalsIgnoreCase(parts[1]);
    }

    private String computeHmac(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Long.toHexString(data.hashCode());
        }
    }

    private synchronized void loadLicensesFromDisk() {
        if (!storageFile.exists()) {
            return;
        }
        try {
            List<LicenseInfo> list = objectMapper.readValue(storageFile, new TypeReference<List<LicenseInfo>>() {});
            for (LicenseInfo li : list) {
                if (li.getLicenseKey() != null) {
                    licenses.put(li.getLicenseKey(), li);
                }
            }
            LOGGER.info(() -> "Loaded " + licenses.size() + " licenses from " + storageFile.getPath());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load licenses from " + storageFile.getPath(), e);
        }
    }

    private synchronized void saveLicensesToDisk() {
        try {
            File parent = storageFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            objectMapper.writeValue(storageFile, new ArrayList<>(licenses.values()));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to persist licenses to " + storageFile.getPath(), e);
        }
    }
}
