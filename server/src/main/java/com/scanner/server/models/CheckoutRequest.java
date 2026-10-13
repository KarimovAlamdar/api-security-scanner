package com.scanner.server.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request payload for creating a Stripe Checkout Session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutRequest {
    private String tier = "Pro";                // Starter, Pro, Enterprise
    private String billingCycle = "monthly";    // monthly, annual
    private String couponCode;                  // e.g. EARLYBIRD10
    private String customerEmail;               // buyer's email
    private String successUrl;                  // URL redirect on payment success
    private String cancelUrl;                   // URL redirect on cancellation

    public CheckoutRequest() {
    }

    public CheckoutRequest(String tier, String billingCycle, String couponCode, String customerEmail, String successUrl, String cancelUrl) {
        this.tier = tier;
        this.billingCycle = billingCycle;
        this.couponCode = couponCode;
        this.customerEmail = customerEmail;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
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

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    @Override
    public String toString() {
        return "CheckoutRequest{" +
                "tier='" + tier + '\'' +
                ", billingCycle='" + billingCycle + '\'' +
                ", couponCode='" + couponCode + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                '}';
    }
}
