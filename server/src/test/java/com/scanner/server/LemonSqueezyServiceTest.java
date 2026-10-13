package com.scanner.server;

import com.scanner.server.models.CheckoutRequest;
import com.scanner.server.models.CheckoutResponse;
import com.scanner.server.service.LemonSqueezyService;
import com.scanner.server.service.LicenseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LemonSqueezyServiceTest {

    @TempDir
    Path tempDir;

    private LicenseManager licenseManager;
    private LemonSqueezyService lemonSqueezyService;

    @BeforeEach
    void setUp() {
        File tempStore = tempDir.resolve("test-licenses.json").toFile();
        licenseManager = new LicenseManager(tempStore.getAbsolutePath(), "test-hmac-secret");
        lemonSqueezyService = new LemonSqueezyService(
                "", "", "ls_whsec_test_secret",
                Collections.emptyMap(), "http://localhost:3000", licenseManager
        );
    }

    @Test
    void testLemonSqueezyCheckoutSimulationWithCoupon() {
        CheckoutRequest req = new CheckoutRequest("Pro", "monthly", "EARLYBIRD10", "founder@az-saas.com", "", "");
        CheckoutResponse resp = lemonSqueezyService.createCheckoutSession(req);

        assertTrue(resp.isSuccess());
        assertEquals("Pro", resp.getPlan());
        assertTrue(resp.isDiscountApplied());
        // Normal Pro is 4900 cents ($49). With 50% discount it's 2450 cents ($24.50)
        assertEquals(2450L, resp.getAmountCents());
        assertNotNull(resp.getSessionId());
        assertTrue(resp.getSessionId().startsWith("ls_order_sim_"));
        assertNotNull(resp.getSimulatedLicenseKey());
        assertTrue(resp.getSimulatedLicenseKey().startsWith("sec_live_"));

        // Verify that the license is already registered and valid in licenseManager
        var verify = licenseManager.verifyLicense(resp.getSimulatedLicenseKey());
        assertTrue(verify.isValid());
        assertEquals("Pro", verify.getLicense().getTier());
        assertEquals("founder@az-saas.com", verify.getLicense().getCustomerEmail());
    }

    @Test
    void testLemonSqueezyWebhookOrderCreated() throws Exception {
        String webhookPayload = """
                {
                  "meta": {
                    "event_name": "order_created",
                    "custom_data": {
                      "tier": "Enterprise",
                      "billing_cycle": "annual"
                    }
                  },
                  "data": {
                    "id": "ls_order_real_9999",
                    "attributes": {
                      "user_email": "enterprise-lead@bigcorp.io",
                      "customer_id": 883491,
                      "first_order_item": {
                        "product_name": "API Security Scanner (Enterprise Annual)"
                      }
                    }
                  }
                }
                """;

        Map<String, Object> result = lemonSqueezyService.processWebhookEvent(webhookPayload);
        assertTrue((Boolean) result.get("received"));
        assertEquals("order_created", result.get("eventName"));
        assertNotNull(result.get("licenseKey"));

        String key = (String) result.get("licenseKey");
        assertTrue(key.startsWith("sec_live_"));

        var verify = licenseManager.verifyLicense(key);
        assertTrue(verify.isValid());
        assertEquals("Enterprise", verify.getLicense().getTier());
        assertEquals("enterprise-lead@bigcorp.io", verify.getLicense().getCustomerEmail());
        assertEquals(9999, verify.getLicense().getMaxRepos());
    }
}
