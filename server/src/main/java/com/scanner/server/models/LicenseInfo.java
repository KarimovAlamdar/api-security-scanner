package com.scanner.server.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Model representing an issued customer license for the API Security Scanner.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseInfo {
    private String licenseKey;          // sec_live_...
    private String customerEmail;       // customer@example.com
    private String tier;                // Starter, Pro, Enterprise
    private String billingCycle;        // monthly, annual
    private String status;              // ACTIVE, EXPIRED, REVOKED
    private int maxRepos;               // Allowed active repositories
    private List<String> features;      // e.g. ["DAST", "SAST_MAPPING", "AUTO_PR_BOT", "SHADOW_API_DISCOVERY", "STATEFUL_BOLA"]
    private String createdAt;           // ISO-8601 timestamp
    private String expiresAt;           // ISO-8601 timestamp
    private String stripeSessionId;     // Associated Stripe Checkout Session ID
    private String stripeCustomerId;    // Associated Stripe Customer ID

    public LicenseInfo() {
    }

    public LicenseInfo(String licenseKey, String customerEmail, String tier, String billingCycle, 
                       String status, int maxRepos, List<String> features, 
                       String createdAt, String expiresAt, String stripeSessionId, String stripeCustomerId) {
        this.licenseKey = licenseKey;
        this.customerEmail = customerEmail;
        this.tier = tier;
        this.billingCycle = billingCycle;
        this.status = status;
        this.maxRepos = maxRepos;
        this.features = features;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.stripeSessionId = stripeSessionId;
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMaxRepos() {
        return maxRepos;
    }

    public void setMaxRepos(int maxRepos) {
        this.maxRepos = maxRepos;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }
}
