package com.scanner;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LicenseValidatorTest {

    @Test
    void testCommunityTierWhenNoLicenseKey() {
        LicenseValidator.LicenseStatus status = LicenseValidator.validate(null, "http://localhost:4242", false);

        assertTrue(status.valid());
        assertEquals("Community", status.tier());
        assertEquals(1, status.maxRepos());
        assertTrue(status.features().contains("DAST_CORE"));
        assertTrue(status.features().contains("SARIF_EXPORT"));
        assertFalse(status.features().contains("AUTO_PR_BOT"));
    }

    @Test
    void testInvalidLicenseKeyFormat() {
        LicenseValidator.LicenseStatus status = LicenseValidator.validate("invalid_random_string", "http://localhost:4242", false);

        assertFalse(status.valid());
        assertTrue(status.message().contains("Must start with 'sec_live_'"));
    }

    @Test
    void testAirGappedOfflineVerification() {
        // A valid offline key structure: sec_live_<token>_<hmac10>
        // Generated using default secret "api-security-scanner-secret-hmac-key-2026"
        LicenseValidator.LicenseStatus badStatus = LicenseValidator.validate("sec_live_1234567890abcdef_badchecksum", "http://localhost:4242", true);
        assertFalse(badStatus.valid());
    }

    @Test
    void testAirGappedValidOfflineVerification() throws Exception {
        String secret = "api-security-scanner-secret-hmac-key-2026";
        String token = "pro123456789012345678901";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        String validKey = "sec_live_" + token + "_" + sb.substring(0, 10);

        LicenseValidator.LicenseStatus status = LicenseValidator.validate(validKey, "http://localhost:4242", true);
        assertTrue(status.valid());
        assertEquals("Pro", status.tier());
        assertTrue(status.isFeatureAllowed("AUTO_PR_BOT"));
        assertTrue(status.isFeatureAllowed("STATEFUL_BOLA"));
    }

    @Test
    void testFeatureGateEnforcement() {
        LicenseValidator.LicenseStatus community = new LicenseValidator.LicenseStatus(
                true, "Community", "user@test.org", 1, Set.of("DAST_CORE", "SARIF_EXPORT"), "Community mode", true
        );

        // Community plan should fail if user requests --auto-pr
        assertThrows(IllegalArgumentException.class, () -> {
            LicenseValidator.enforceFeatureGate(community, true, false);
        });

        // Pro plan allows auto-pr
        LicenseValidator.LicenseStatus pro = new LicenseValidator.LicenseStatus(
                true, "Pro", "pro@company.com", 15, Set.of("DAST_CORE", "SAST_MAPPING", "AUTO_PR_BOT", "STATEFUL_BOLA"), "Pro active", false
        );
        assertDoesNotThrow(() -> {
            LicenseValidator.enforceFeatureGate(pro, true, true);
        });
    }
}
