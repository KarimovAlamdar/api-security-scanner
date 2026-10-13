package com.scanner;

import com.scanner.ApiSecurityScanner.GithubConfig;
import com.scanner.ApiSecurityScanner.Severity;

import java.time.Duration;
import java.util.Set;

/**
 * Scan configuration including authentication credentials, dual-role tokens,
 * license key verification, network parameters, and output report paths.
 */
public record Config(
        String specPath,
        String baseUrl,
        String authHeader,
        String userAToken,
        String userBToken,
        String userAHeader,
        String userBHeader,
        Set<String> methods,
        Severity failOn,
        Duration timeout,
        String reportPath,
        String sarifPath,
        String scanSourcePath,
        boolean dryRun,
        GithubConfig github,
        int concurrency,
        long requestDelayMs,
        String htmlReportPath,
        String slackWebhook,
        String teamsWebhook,
        OAuthConfig oauth,
        boolean autoPr,
        boolean airGapped,
        String licenseKey,
        String licenseServerUrl
) {
    public record OAuthConfig(String tokenUrl, String clientId, String clientSecret, String scope) {
        public boolean isConfigured() {
            return tokenUrl != null && !tokenUrl.isBlank()
                    && clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }

    public Config(
            String specPath,
            String baseUrl,
            String authHeader,
            String userAToken,
            String userBToken,
            String userAHeader,
            String userBHeader,
            Set<String> methods,
            Severity failOn,
            Duration timeout,
            String reportPath,
            String sarifPath,
            String scanSourcePath,
            boolean dryRun,
            GithubConfig github
    ) {
        this(specPath, baseUrl, authHeader, userAToken, userBToken, userAHeader, userBHeader,
                methods, failOn, timeout, reportPath, sarifPath, scanSourcePath, dryRun, github, 4, 0L,
                "scan-report.html", null, null, new OAuthConfig(null, null, null, null), false, false, null, "http://localhost:4242");
    }

    public Config(
            String specPath,
            String baseUrl,
            String authHeader,
            String userAToken,
            String userBToken,
            String userAHeader,
            String userBHeader,
            Set<String> methods,
            Severity failOn,
            Duration timeout,
            String reportPath,
            String sarifPath,
            String scanSourcePath,
            boolean dryRun,
            GithubConfig github,
            int concurrency,
            long requestDelayMs
    ) {
        this(specPath, baseUrl, authHeader, userAToken, userBToken, userAHeader, userBHeader,
                methods, failOn, timeout, reportPath, sarifPath, scanSourcePath, dryRun, github,
                concurrency, requestDelayMs, "scan-report.html", null, null, new OAuthConfig(null, null, null, null), false, false, null, "http://localhost:4242");
    }

    public Config(
            String specPath,
            String baseUrl,
            String authHeader,
            String userAToken,
            String userBToken,
            String userAHeader,
            String userBHeader,
            Set<String> methods,
            Severity failOn,
            Duration timeout,
            String reportPath,
            String sarifPath,
            String scanSourcePath,
            boolean dryRun,
            GithubConfig github,
            int concurrency,
            long requestDelayMs,
            String htmlReportPath,
            String slackWebhook,
            String teamsWebhook,
            OAuthConfig oauth
    ) {
        this(specPath, baseUrl, authHeader, userAToken, userBToken, userAHeader, userBHeader,
                methods, failOn, timeout, reportPath, sarifPath, scanSourcePath, dryRun, github,
                concurrency, requestDelayMs, htmlReportPath, slackWebhook, teamsWebhook, oauth, false, false, null, "http://localhost:4242");
    }

    public Config(
            String specPath,
            String baseUrl,
            String authHeader,
            String userAToken,
            String userBToken,
            String userAHeader,
            String userBHeader,
            Set<String> methods,
            Severity failOn,
            Duration timeout,
            String reportPath,
            String sarifPath,
            String scanSourcePath,
            boolean dryRun,
            GithubConfig github,
            int concurrency,
            long requestDelayMs,
            String htmlReportPath,
            String slackWebhook,
            String teamsWebhook,
            OAuthConfig oauth,
            boolean autoPr,
            boolean airGapped
    ) {
        this(specPath, baseUrl, authHeader, userAToken, userBToken, userAHeader, userBHeader,
                methods, failOn, timeout, reportPath, sarifPath, scanSourcePath, dryRun, github,
                concurrency, requestDelayMs, htmlReportPath, slackWebhook, teamsWebhook, oauth, autoPr, airGapped, null, "http://localhost:4242");
    }

    public boolean hasDualRoleTokens() {
        return userAToken != null && !userAToken.isBlank()
                && userBToken != null && !userBToken.isBlank();
    }
}
