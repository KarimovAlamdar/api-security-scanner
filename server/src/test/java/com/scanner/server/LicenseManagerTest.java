package com.scanner.server;

import com.scanner.server.models.LicenseInfo;
import com.scanner.server.models.LicenseVerifyResponse;
import com.scanner.server.service.LicenseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LicenseManagerTest {

    @TempDir
    Path tempDir;

    private LicenseManager licenseManager;

    @BeforeEach
    void setUp() {
        File tempStore = tempDir.resolve("test-licenses.json").toFile();
        licenseManager = new LicenseManager(tempStore.getAbsolutePath(), "test-hmac-secret-12345");
    }

    @Test
    void testIssueAndVerifyLicense() {
        LicenseInfo license = licenseManager.issueLicense("test@company.com", "Pro", "monthly", "cs_test_123", "cus_test_123");

        assertNotNull(license);
        assertNotNull(license.getLicenseKey());
        assertTrue(license.getLicenseKey().startsWith("sec_live_"));
        assertEquals("test@company.com", license.getCustomerEmail());
        assertEquals("Pro", license.getTier());
        assertEquals("ACTIVE", license.getStatus());
        assertEquals(15, license.getMaxRepos());
        assertTrue(license.getFeatures().contains("AUTO_PR_BOT"));
        assertTrue(license.getFeatures().contains("STATEFUL_BOLA"));
        assertTrue(license.getFeatures().contains("SHADOW_API_DISCOVERY"));

        // Verify with valid key
        LicenseVerifyResponse verifyResp = licenseManager.verifyLicense(license.getLicenseKey());
        assertTrue(verifyResp.isValid());
        assertEquals("Pro", verifyResp.getLicense().getTier());

        // Verify with invalid key
        LicenseVerifyResponse invalidResp = licenseManager.verifyLicense("sec_live_invalid_bogus_key");
        assertFalse(invalidResp.isValid());
    }

    @Test
    void testEnterpriseTierFeatures() {
        LicenseInfo entLicense = licenseManager.issueLicense("cto@enterprise.com", "Enterprise", "annual", "cs_ent_999", "cus_ent_999");
        assertEquals(9999, entLicense.getMaxRepos());
        assertTrue(entLicense.getFeatures().contains("CUSTOM_RULES"));
        assertTrue(entLicense.getFeatures().contains("PRIORITY_SLA"));

        LicenseVerifyResponse verify = licenseManager.verifyLicense(entLicense.getLicenseKey());
        assertTrue(verify.isValid());
    }
}
