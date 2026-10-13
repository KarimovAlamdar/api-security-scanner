package com.scanner.server.models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response payload for license verification requests made by Scanner CLI or GitHub Action.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LicenseVerifyResponse {
    private boolean valid;
    private String message;
    private LicenseInfo license;

    public LicenseVerifyResponse() {
    }

    public static LicenseVerifyResponse valid(LicenseInfo license) {
        LicenseVerifyResponse r = new LicenseVerifyResponse();
        r.valid = true;
        r.message = "License is active and valid for plan: " + license.getTier();
        r.license = license;
        return r;
    }

    public static LicenseVerifyResponse invalid(String reason) {
        LicenseVerifyResponse r = new LicenseVerifyResponse();
        r.valid = false;
        r.message = "License verification failed: " + reason;
        return r;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LicenseInfo getLicense() {
        return license;
    }

    public void setLicense(LicenseInfo license) {
        this.license = license;
    }
}
