package com.scanner.server.models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response payload returned after creating a checkout session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutResponse {
    private boolean success;
    private String sessionId;
    private String checkoutUrl;
    private String plan;
    private String billingCycle;
    private long amountCents;
    private String currency;
    private boolean discountApplied;
    private String discountDetails;
    private String mode; // "stripe_live", "stripe_test", "simulation"
    private String message;
    private String simulatedLicenseKey; // Provided only in sandbox simulation mode

    public CheckoutResponse() {
    }

    public static CheckoutResponse success(String sessionId, String checkoutUrl, String plan, 
                                          String billingCycle, long amountCents, boolean discountApplied, 
                                          String mode, String message) {
        CheckoutResponse r = new CheckoutResponse();
        r.success = true;
        r.sessionId = sessionId;
        r.checkoutUrl = checkoutUrl;
        r.plan = plan;
        r.billingCycle = billingCycle;
        r.amountCents = amountCents;
        r.currency = "USD";
        r.discountApplied = discountApplied;
        r.mode = mode;
        r.message = message;
        return r;
    }

    public static CheckoutResponse error(String message) {
        CheckoutResponse r = new CheckoutResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(boolean discountApplied) {
        this.discountApplied = discountApplied;
    }

    public String getDiscountDetails() {
        return discountDetails;
    }

    public void setDiscountDetails(String discountDetails) {
        this.discountDetails = discountDetails;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSimulatedLicenseKey() {
        return simulatedLicenseKey;
    }

    public void setSimulatedLicenseKey(String simulatedLicenseKey) {
        this.simulatedLicenseKey = simulatedLicenseKey;
    }
}
