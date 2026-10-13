package com.scanner.server;

import com.scanner.server.models.CheckoutRequest;
import com.scanner.server.models.CheckoutResponse;
import com.scanner.server.service.LicenseManager;
import com.scanner.server.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StripeServiceTest {

    @TempDir
    Path tempDir;

    private LicenseManager licenseManager;
    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        File tempStore = tempDir.resolve("test-licenses.json").toFile();
        licenseManager = new LicenseManager(tempStore.getAbsolutePath(), "test-hmac-secret");
        // Initialize without real keys to test simulation/test mode
        stripeService = new StripeService("", "", "http://localhost:3000", licenseManager);
    }

    @Test
    void testCreateCheckoutSessionWithEarlyBirdDiscount() {
        CheckoutRequest req = new CheckoutRequest("Pro", "monthly", "EARLYBIRD10", "dev@startup.io", "", "");
        CheckoutResponse resp = stripeService.createCheckoutSession(req);

        assertTrue(resp.isSuccess());
        assertEquals("Pro", resp.getPlan());
        assertEquals("monthly", resp.getBillingCycle());
        assertTrue(resp.isDiscountApplied());
        // Normal Pro monthly is 4900 cents ($49). With 50% discount it should be 2450 cents ($24.50)
        assertEquals(2450L, resp.getAmountCents());
        assertNotNull(resp.getSessionId());
        assertNotNull(resp.getCheckoutUrl());
        assertNotNull(resp.getSimulatedLicenseKey());
        assertTrue(resp.getSimulatedLicenseKey().startsWith("sec_live_"));
    }

    @Test
    void testCreateCheckoutSessionAnnualWithoutDiscount() {
        CheckoutRequest req = new CheckoutRequest("Starter", "annual", "", "lead@agency.com", "", "");
        CheckoutResponse resp = stripeService.createCheckoutSession(req);

        assertTrue(resp.isSuccess());
        assertEquals("Starter", resp.getPlan());
        assertEquals("annual", resp.getBillingCycle());
        assertFalse(resp.isDiscountApplied());
        // Starter annual is 18000 cents ($180/year = $15/mo)
        assertEquals(18000L, resp.getAmountCents());
    }

    @Test
    void testWebhookEventProcessing() throws Exception {
        String webhookPayload = """
                {
                  "id": "evt_test_123",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_stripe_real_session_789",
                      "customer": "cus_stripe_real_customer_456",
                      "customer_details": {
                        "email": "buyer@enterprise.corp"
                      },
                      "metadata": {
                        "tier": "Enterprise",
                        "billingCycle": "annual"
                      }
                    }
                  }
                }
                """;

        Map<String, Object> result = stripeService.processWebhookEvent(webhookPayload);
        assertTrue((Boolean) result.get("received"));
        assertEquals("checkout.session.completed", result.get("eventType"));
        assertNotNull(result.get("licenseKey"));

        String licenseKey = (String) result.get("licenseKey");
        assertTrue(licenseKey.startsWith("sec_live_"));

        // Check that license manager now has this license
        var verify = licenseManager.verifyLicense(licenseKey);
        assertTrue(verify.isValid());
        assertEquals("Enterprise", verify.getLicense().getTier());
        assertEquals("buyer@enterprise.corp", verify.getLicense().getCustomerEmail());
    }
}
