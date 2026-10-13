package com.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ApiSecurityScannerTest {

    @Test
    void testConfigParsingWithNewFlags() {
        String[] args = {
                "--spec", "openapi.json",
                "--base-url", "http://localhost:8080",
                "--user-a-token", "tokenA123",
                "--user-b-token", "tokenB456",
                "--sarif-output", "custom-report.sarif",
                "--scan-source", "./demo",
                "--fail-on", "critical"
        };

        Config cfg = ApiSecurityScanner.parseArgs(args);
        assertNotNull(cfg);
        assertEquals("tokenA123", cfg.userAToken());
        assertEquals("Bearer tokenA123", cfg.userAHeader());
        assertEquals("tokenB456", cfg.userBToken());
        assertEquals("Bearer tokenB456", cfg.userBHeader());
        assertEquals("custom-report.sarif", cfg.sarifPath());
        assertEquals("./demo", cfg.scanSourcePath());
        assertEquals(ApiSecurityScanner.Severity.CRITICAL, cfg.failOn());
        assertTrue(cfg.hasDualRoleTokens());
    }

    @Test
    void testConfigBackwardsCompatibilitySingleToken() {
        String[] args = {
                "--spec", "openapi.json",
                "--base-url", "http://localhost:8080",
                "--auth-token", "legacyToken"
        };

        Config cfg = ApiSecurityScanner.parseArgs(args);
        assertNotNull(cfg);
        assertEquals("legacyToken", cfg.userAToken());
        assertEquals("Bearer legacyToken", cfg.authHeader());
        assertEquals("Bearer legacyToken", cfg.userAHeader());
        assertNull(cfg.userBToken());
        assertNull(cfg.userBHeader());
        assertFalse(cfg.hasDualRoleTokens());
        assertEquals("scan-report.sarif", cfg.sarifPath());
        assertEquals(".", cfg.scanSourcePath());
    }

    @Test
    void testSourceCodeMapperSpringAnnotations(@TempDir Path tempDir) throws IOException {
        String javaSource = """
                package com.example;
                import org.springframework.web.bind.annotation.*;

                @RestController
                @RequestMapping("/api/v1/users")
                public class TestUserController {

                    @GetMapping("/{userId}")
                    public String getUser(@PathVariable String userId) {
                        return userId;
                    }

                    @PostMapping
                    public String createUser() {
                        return "created";
                    }

                    @DeleteMapping("/{userId}")
                    public void deleteUser(@PathVariable String userId) {
                    }
                }
                """;

        Path file = tempDir.resolve("TestUserController.java");
        Files.writeString(file, javaSource);

        SourceCodeMapper mapper = new SourceCodeMapper();
        mapper.scanDirectory(tempDir);

        assertFalse(mapper.getMappedLocations().isEmpty());

        // Match GET /api/v1/users/{id} against /api/v1/users/{userId}
        SourceCodeMapper.SourceLocation locGet = mapper.findLocation("GET", "/api/v1/users/{id}");
        assertNotNull(locGet);
        assertEquals("GET", locGet.httpMethod());
        assertTrue(locGet.routePath().contains("/api/v1/users/{userId}"));
        assertTrue(locGet.lineNumber() > 0);

        // Match DELETE /api/v1/users/{id}
        SourceCodeMapper.SourceLocation locDelete = mapper.findLocation("DELETE", "/api/v1/users/{id}");
        assertNotNull(locDelete);
        assertEquals("DELETE", locDelete.httpMethod());

        // Match POST /api/v1/users
        SourceCodeMapper.SourceLocation locPost = mapper.findLocation("POST", "/api/v1/users");
        assertNotNull(locPost);
        assertEquals("POST", locPost.httpMethod());
    }

    @Test
    void testSourceCodeMapperScansWorkspaceDemo() {
        SourceCodeMapper mapper = new SourceCodeMapper("demo");
        assertFalse(mapper.getMappedLocations().isEmpty());

        SourceCodeMapper.SourceLocation loc = mapper.findLocation("GET", "/users/{id}");
        assertNotNull(loc);
        assertTrue(loc.filePath().contains("UserController.java"));
        assertTrue(loc.lineNumber() > 0);
    }

    @Test
    void testRemediationSuggesterKnownVulnerabilities() {
        String sqliPatch = RemediationSuggester.suggest("SQLI_INDICATOR", "GET", "/users", "query:search");
        assertNotNull(sqliPatch);
        assertTrue(sqliPatch.contains("PreparedStatement") || sqliPatch.contains("@Query"));

        String authPatch = RemediationSuggester.suggest("AUTH_BYPASS", "DELETE", "/users/{id}", "header:Authorization");
        assertNotNull(authPatch);
        assertTrue(authPatch.contains("@PreAuthorize"));

        String corsPatch = RemediationSuggester.suggest("CORS_WILDCARD", "GET", "/products", "header:Origin");
        assertNotNull(corsPatch);
        assertTrue(corsPatch.contains("@CrossOrigin"));

        String bolaPatch = RemediationSuggester.suggest("BOLA_IDOR_VULNERABILITY", "GET", "/users/{id}", "header:Authorization");
        assertNotNull(bolaPatch);
        assertTrue(bolaPatch.contains("@PreAuthorize") && bolaPatch.contains("ResourceOwner"));
    }

    @Test
    void testSarifExporterValidation(@TempDir Path tempDir) throws IOException {
        Path sarifFile = tempDir.resolve("test-report.sarif");

        Finding f1 = new Finding(
                "BOLA_IDOR_VULNERABILITY",
                ApiSecurityScanner.Severity.CRITICAL,
                "GET",
                "/users/{id}",
                "header:Authorization",
                "User B accessed User A resource without authorization",
                "curl -i -X GET 'http://localhost:8080/users/1' -H 'Authorization: Bearer <API_TOKEN>'",
                "demo/UserController.java",
                24,
                "@PreAuthorize(\"@securityService.isResourceOwner(authentication, #id)\")"
        );

        Finding f2 = new Finding(
                "SQLI_INDICATOR",
                ApiSecurityScanner.Severity.HIGH,
                "GET",
                "/users",
                "query:search",
                "SQL syntax error leaked",
                "curl -i -X GET 'http://localhost:8080/users?search=%27'",
                "demo/UserController.java",
                14,
                RemediationSuggester.suggest("SQLI_INDICATOR", "GET", "/users", "query:search")
        );

        SarifExporter.writeSarif(
                sarifFile.toString(),
                "openapi.json",
                "http://localhost:8080",
                "1.2.0",
                List.of(f1, f2)
        );

        assertTrue(Files.exists(sarifFile));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(sarifFile.toFile());

        assertEquals("2.1.0", root.path("version").asText());
        assertEquals("https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
                root.path("$schema").asText());

        JsonNode runs = root.path("runs");
        assertEquals(1, runs.size());

        JsonNode driver = runs.get(0).path("tool").path("driver");
        assertEquals("api-security-scanner", driver.path("name").asText());
        assertEquals("1.2.0", driver.path("semanticVersion").asText());

        JsonNode results = runs.get(0).path("results");
        assertEquals(2, results.size());

        // Check first result (BOLA)
        JsonNode r1 = results.get(0);
        assertEquals("BOLA_IDOR_VULNERABILITY", r1.path("ruleId").asText());
        assertEquals("error", r1.path("level").asText());

        JsonNode physLoc1 = r1.path("locations").get(0).path("physicalLocation");
        assertEquals("demo/UserController.java", physLoc1.path("artifactLocation").path("uri").asText());
        assertEquals(24, physLoc1.path("region").path("startLine").asInt());

        JsonNode fixes1 = r1.path("fixes");
        assertFalse(fixes1.isEmpty());
        assertTrue(fixes1.get(0).path("fileChanges").get(0).path("replacements").get(0)
                .path("insertedContent").path("text").asText().contains("@PreAuthorize"));
    }

    // ------------------------------------------------------------------ Phase 1 (P0) Tests

    @Test
    void testRemediationSuggesterPhase1Vulnerabilities() {
        // XSS
        String xssPatch = RemediationSuggester.suggest("XSS_REFLECTED", "GET", "/search", "query:q");
        assertNotNull(xssPatch);
        assertTrue(xssPatch.contains("HtmlUtils") || xssPatch.contains("htmlEscape"));

        // Security Headers
        String hstsPatch = RemediationSuggester.suggest("MISSING_HSTS", "GET", "/api", "header:Strict-Transport-Security");
        assertNotNull(hstsPatch);
        assertTrue(hstsPatch.contains("httpStrictTransportSecurity") || hstsPatch.contains("maxAgeInSeconds"));

        String cspPatch = RemediationSuggester.suggest("MISSING_CSP", "GET", "/api", "header:Content-Security-Policy");
        assertNotNull(cspPatch);
        assertTrue(cspPatch.contains("contentSecurityPolicy") || cspPatch.contains("default-src"));

        String xctoPatch = RemediationSuggester.suggest("MISSING_XCTO", "GET", "/api", "header:X-Content-Type-Options");
        assertNotNull(xctoPatch);
        assertTrue(xctoPatch.contains("contentTypeOptions") || xctoPatch.contains("nosniff"));

        String xfoPatch = RemediationSuggester.suggest("MISSING_XFO", "GET", "/api", "header:X-Frame-Options");
        assertNotNull(xfoPatch);
        assertTrue(xfoPatch.contains("frameOptions") || xfoPatch.contains("deny"));

        String serverPatch = RemediationSuggester.suggest("SERVER_VERSION_LEAK", "GET", "/api", "header:Server");
        assertNotNull(serverPatch);
        assertTrue(serverPatch.contains("server-header") || serverPatch.contains("server"));

        String poweredByPatch = RemediationSuggester.suggest("TECH_STACK_LEAK", "GET", "/api", "header:X-Powered-By");
        assertNotNull(poweredByPatch);
        assertTrue(poweredByPatch.contains("X-Powered-By"));

        // Cookie Flags
        String httpOnlyPatch = RemediationSuggester.suggest("COOKIE_NO_HTTPONLY", "GET", "/api", "header:Set-Cookie");
        assertNotNull(httpOnlyPatch);
        assertTrue(httpOnlyPatch.contains("setHttpOnly") || httpOnlyPatch.contains("http-only"));

        String securePatch = RemediationSuggester.suggest("COOKIE_NO_SECURE", "GET", "/api", "header:Set-Cookie");
        assertNotNull(securePatch);
        assertTrue(securePatch.contains("setSecure") || securePatch.contains("secure"));

        String sameSitePatch = RemediationSuggester.suggest("COOKIE_NO_SAMESITE", "GET", "/api", "header:Set-Cookie");
        assertNotNull(sameSitePatch);
        assertTrue(sameSitePatch.contains("SameSite") || sameSitePatch.contains("sameSite"));
    }

    @Test
    void testSarifExporterIncludesPhase1Rules(@TempDir Path tempDir) throws IOException {
        Path sarifFile = tempDir.resolve("rules-check.sarif");

        // Write SARIF with an XSS finding to ensure new rules are included
        Finding xssFind = new Finding(
                "XSS_REFLECTED",
                ApiSecurityScanner.Severity.HIGH,
                "GET",
                "/search",
                "query:q",
                "Reflected XSS: payload was echoed unencoded",
                "curl -i -X GET 'http://localhost:8080/search?q=%3Cscript%3Ealert(1)%3C/script%3E'"
        );

        SarifExporter.writeSarif(
                sarifFile.toString(), "openapi.json", "http://localhost:8080", "1.2.0",
                List.of(xssFind)
        );

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(sarifFile.toFile());
        JsonNode rules = root.path("runs").get(0).path("tool").path("driver").path("rules");

        // Should have 30 rules total (7 original + 10 Phase 1 + 4 Phase 2 + 4 Phase 3 + 5 Killer Feature rules)
        assertEquals(30, rules.size(), "Expected 30 SARIF rules");

        // Verify rule IDs are present
        Set<String> ruleIds = new java.util.HashSet<>();
        for (JsonNode rule : rules) {
            ruleIds.add(rule.path("id").asText());
        }
        assertTrue(ruleIds.contains("XSS_REFLECTED"), "Missing XSS_REFLECTED rule");
        assertTrue(ruleIds.contains("MISSING_HSTS"), "Missing MISSING_HSTS rule");
        assertTrue(ruleIds.contains("MISSING_CSP"), "Missing MISSING_CSP rule");
        assertTrue(ruleIds.contains("MISSING_XCTO"), "Missing MISSING_XCTO rule");
        assertTrue(ruleIds.contains("MISSING_XFO"), "Missing MISSING_XFO rule");
        assertTrue(ruleIds.contains("SERVER_VERSION_LEAK"), "Missing SERVER_VERSION_LEAK rule");
        assertTrue(ruleIds.contains("TECH_STACK_LEAK"), "Missing TECH_STACK_LEAK rule");
        assertTrue(ruleIds.contains("COOKIE_NO_HTTPONLY"), "Missing COOKIE_NO_HTTPONLY rule");
        assertTrue(ruleIds.contains("COOKIE_NO_SECURE"), "Missing COOKIE_NO_SECURE rule");
        assertTrue(ruleIds.contains("COOKIE_NO_SAMESITE"), "Missing COOKIE_NO_SAMESITE rule");
        // Phase 2 rules
        assertTrue(ruleIds.contains("SSRF_DETECTED"), "Missing SSRF_DETECTED rule");
        assertTrue(ruleIds.contains("SSRF_POTENTIAL"), "Missing SSRF_POTENTIAL rule");
        assertTrue(ruleIds.contains("PATH_TRAVERSAL"), "Missing PATH_TRAVERSAL rule");
        assertTrue(ruleIds.contains("SQLI_BLIND_TIME"), "Missing SQLI_BLIND_TIME rule");
        // Phase 3 rules
        assertTrue(ruleIds.contains("COMMAND_INJECTION"), "Missing COMMAND_INJECTION rule");
        assertTrue(ruleIds.contains("SSTI_DETECTED"), "Missing SSTI_DETECTED rule");
        assertTrue(ruleIds.contains("OPEN_REDIRECT"), "Missing OPEN_REDIRECT rule");
        assertTrue(ruleIds.contains("NOSQL_INJECTION"), "Missing NOSQL_INJECTION rule");
        // Killer Feature rules
        assertTrue(ruleIds.contains("SHADOW_API_EXPOSED"), "Missing SHADOW_API_EXPOSED rule");
        assertTrue(ruleIds.contains("ZOMBIE_API_ENDPOINT"), "Missing ZOMBIE_API_ENDPOINT rule");
        assertTrue(ruleIds.contains("STATEFUL_BOLA_CONFIRMED"), "Missing STATEFUL_BOLA_CONFIRMED rule");
        assertTrue(ruleIds.contains("LLM_SYSTEM_PROMPT_LEAK"), "Missing LLM_SYSTEM_PROMPT_LEAK rule");
        assertTrue(ruleIds.contains("LLM_PROMPT_INJECTION"), "Missing LLM_PROMPT_INJECTION rule");
    }

    @Test
    void testPhase2RemediationSuggestions() {
        String ssrfPatch = RemediationSuggester.suggest("SSRF_DETECTED", "POST", "/webhook", "body:url");
        assertNotNull(ssrfPatch);
        assertTrue(ssrfPatch.contains("InetAddress") || ssrfPatch.contains("allowlist") || ssrfPatch.contains("SSRF"));

        String ssrfPotPatch = RemediationSuggester.suggest("SSRF_POTENTIAL", "GET", "/proxy", "query:target");
        assertNotNull(ssrfPotPatch);
        assertTrue(ssrfPotPatch.contains("allowlist") || ssrfPotPatch.contains("InetAddress"));

        String pathPatch = RemediationSuggester.suggest("PATH_TRAVERSAL", "GET", "/files", "query:file");
        assertNotNull(pathPatch);
        assertTrue(pathPatch.contains("normalize") || pathPatch.contains("startsWith"));

        String blindSqliPatch = RemediationSuggester.suggest("SQLI_BLIND_TIME", "GET", "/users", "query:id");
        assertNotNull(blindSqliPatch);
        assertTrue(blindSqliPatch.contains("Query") || blindSqliPatch.contains("PreparedStatement") || blindSqliPatch.contains("parameterized"));
    }

    @Test
    void testPhase3RemediationSuggestions() {
        String cmdPatch = RemediationSuggester.suggest("COMMAND_INJECTION", "POST", "/execute", "body:cmd");
        assertNotNull(cmdPatch);
        assertTrue(cmdPatch.contains("ProcessBuilder") || cmdPatch.contains("Runtime"));

        String sstiPatch = RemediationSuggester.suggest("SSTI_DETECTED", "GET", "/render", "query:template");
        assertNotNull(sstiPatch);
        assertTrue(sstiPatch.contains("Context") || sstiPatch.contains("escape") || sstiPatch.contains("HtmlUtils"));

        String redirectPatch = RemediationSuggester.suggest("OPEN_REDIRECT", "GET", "/login", "query:redirect");
        assertNotNull(redirectPatch);
        assertTrue(redirectPatch.contains("ALLOWED_DOMAINS") || redirectPatch.contains("whitelist") || redirectPatch.contains("URI"));

        String nosqlPatch = RemediationSuggester.suggest("NOSQL_INJECTION", "POST", "/auth/login", "body:username");
        assertNotNull(nosqlPatch);
        assertTrue(nosqlPatch.contains("Criteria") || nosqlPatch.contains("where") || nosqlPatch.contains("BasicDBObject"));
    }

    @Test
    void testConfigParsingPhase2Flags() {
        String[] args = {
                "--spec", "openapi.json",
                "--concurrency", "8",
                "--request-delay", "150"
        };
        Config cfg = ApiSecurityScanner.parseArgs(args);
        assertNotNull(cfg);
        assertEquals(8, cfg.concurrency());
        assertEquals(150L, cfg.requestDelayMs());
    }

    @Test
    void testConfigParsingPhase4Flags() {
        String[] args = {
                "--spec", "openapi.json",
                "--html-output", "reports/dashboard.html",
                "--slack-webhook", "https://hooks.slack.com/services/T00/B00/X00",
                "--teams-webhook", "https://outlook.office.com/webhook/123",
                "--oauth-token-url", "https://auth.example.com/oauth/token",
                "--oauth-client-id", "client123",
                "--oauth-client-secret", "secret456",
                "--oauth-scope", "read:api"
        };
        Config cfg = ApiSecurityScanner.parseArgs(args);
        assertNotNull(cfg);
        assertEquals("reports/dashboard.html", cfg.htmlReportPath());
        assertEquals("https://hooks.slack.com/services/T00/B00/X00", cfg.slackWebhook());
        assertEquals("https://outlook.office.com/webhook/123", cfg.teamsWebhook());
        assertNotNull(cfg.oauth());
        assertTrue(cfg.oauth().isConfigured());
        assertEquals("https://auth.example.com/oauth/token", cfg.oauth().tokenUrl());
        assertEquals("client123", cfg.oauth().clientId());
        assertEquals("secret456", cfg.oauth().clientSecret());
        assertEquals("read:api", cfg.oauth().scope());
    }

    @Test
    void testHtmlReportExporterGeneration(@TempDir Path tempDir) throws IOException {
        Path htmlFile = tempDir.resolve("interactive-dashboard.html");

        Finding f1 = new Finding(
                "COMMAND_INJECTION",
                ApiSecurityScanner.Severity.CRITICAL,
                "POST",
                "/api/ping",
                "body:host",
                "Remote OS Command Injection: sleep delay detected",
                "curl -i -X POST 'http://localhost:8080/api/ping' -d '{\"host\":\"; sleep 5\"}'",
                "demo/PingController.java",
                19,
                RemediationSuggester.suggest("COMMAND_INJECTION", "POST", "/api/ping", "body:host")
        );

        HtmlReportExporter.writeHtmlReport(
                htmlFile.toString(),
                "openapi.json",
                "http://localhost:8080",
                "1.2.0",
                List.of(f1),
                10,
                55
        );

        assertTrue(Files.exists(htmlFile), "HTML report should be created");
        String content = Files.readString(htmlFile);
        assertTrue(content.contains("<!DOCTYPE html>"), "Must be valid HTML5");
        assertTrue(content.contains("COMMAND_INJECTION"), "Should display finding ID");
        assertTrue(content.contains("searchInput"), "Should contain interactive filter search bar");
        assertTrue(content.contains("filterFindings()"), "Should contain filter script");
        assertTrue(content.contains("ProcessBuilder"), "Should contain zero-click remediation patch");
        assertTrue(content.contains("localStorage"), "Should contain triage persistent logic");
    }

    @Test
    void testCurlCommandMasksAuthToken() {
        String curl = ApiSecurityScanner.curlCommand(
                "GET",
                java.net.URI.create("http://localhost:8080/users"),
                java.util.Map.of("Authorization", "Bearer secretToken123", "Content-Type", "application/json"),
                null
        );
        assertNotNull(curl);
        assertFalse(curl.contains("secretToken123"), "Auth token must be masked in curl command");
        assertTrue(curl.contains("<API_TOKEN>"), "Masked placeholder expected");
        assertTrue(curl.contains("application/json"), "Non-auth headers should remain");
    }

    // ------------------------------------------------------------------ Killer Features Tests

    @Test
    void testKillerFeatureRemediationSuggestions() {
        // Feature 1 & 2: Shadow & Zombie API Discovery
        String shadowPatch = RemediationSuggester.suggest("SHADOW_API_EXPOSED", "GET", "/api/v2/internal/admin", null);
        assertNotNull(shadowPatch);
        assertTrue(shadowPatch.contains("@Hidden") || shadowPatch.contains("@Operation") || shadowPatch.contains("OpenAPI"));

        String zombiePatch = RemediationSuggester.suggest("ZOMBIE_API_ENDPOINT", "GET", "/api/v1/legacy-login", null);
        assertNotNull(zombiePatch);
        assertTrue(zombiePatch.contains("@Deprecated") || zombiePatch.contains("@Hidden"));

        // Feature 4: Stateful BOLA / IDOR
        String statefulBolaPatch = RemediationSuggester.suggest("STATEFUL_BOLA_CONFIRMED", "GET", "/api/documents/{id}", null);
        assertNotNull(statefulBolaPatch);
        assertTrue(statefulBolaPatch.contains("@PreAuthorize") || statefulBolaPatch.contains("SecurityContext"));

        // Feature 5: LLM & AI Agent Security
        String promptLeakPatch = RemediationSuggester.suggest("LLM_SYSTEM_PROMPT_LEAK", "POST", "/api/v1/chat", "body:prompt");
        assertNotNull(promptLeakPatch);
        assertTrue(promptLeakPatch.contains("systemPrompt") || promptLeakPatch.contains("sanitize") || promptLeakPatch.contains("guardrail"));

        String promptInjPatch = RemediationSuggester.suggest("LLM_PROMPT_INJECTION", "POST", "/api/v1/chat", "body:message");
        assertNotNull(promptInjPatch);
        assertTrue(promptInjPatch.contains("validatePrompt") || promptInjPatch.contains("guardrail") || promptInjPatch.contains("moderation"));
    }

    @Test
    void testConfigParsingKillerFeatures() {
        String[] args = {
                "--spec", "openapi.json",
                "--auto-pr",
                "--air-gapped"
        };
        Config cfg = ApiSecurityScanner.parseArgs(args);
        assertNotNull(cfg);
        assertTrue(cfg.autoPr());
        assertTrue(cfg.airGapped());

        // Test alias --offline
        String[] offlineArgs = {
                "--spec", "openapi.json",
                "--offline"
        };
        Config cfgOffline = ApiSecurityScanner.parseArgs(offlineArgs);
        assertNotNull(cfgOffline);
        assertTrue(cfgOffline.airGapped());
    }

    @Test
    void testSourceCodeMapperMatchesSpecEndpoint() {
        SourceCodeMapper.SourceLocation locExact = new SourceCodeMapper.SourceLocation(
                "UserController.java", 15, "GET", "/users"
        );
        SourceCodeMapper.SourceLocation locParam = new SourceCodeMapper.SourceLocation(
                "UserController.java", 25, "GET", "/users/{userId}"
        );
        SourceCodeMapper.SourceLocation locShadow = new SourceCodeMapper.SourceLocation(
                "AdminController.java", 10, "POST", "/admin/debug-dump"
        );

        List<String> specPaths = List.of("/users", "/users/{id}", "/products");

        assertTrue(SourceCodeMapper.matchesAnySpecEndpoint(locExact, specPaths),
                "Exact path /users should match spec");
        assertTrue(SourceCodeMapper.matchesAnySpecEndpoint(locParam, specPaths),
                "Param path /users/{userId} should match /users/{id} in spec");
        assertFalse(SourceCodeMapper.matchesAnySpecEndpoint(locShadow, specPaths),
                "Undocumented path /admin/debug-dump should not match spec (Shadow API)");
    }

    @Test
    void testAirGappedHtmlReportComplianceAttestation(@TempDir Path tempDir) throws IOException {
        Path htmlFile = tempDir.resolve("air-gapped-report.html");

        Finding f1 = new Finding(
                "SHADOW_API_EXPOSED",
                ApiSecurityScanner.Severity.HIGH,
                "GET",
                "/api/internal/debug",
                "route",
                "Shadow API detected: Route present in Java controller code but missing from OpenAPI spec",
                "curl -i -X GET 'http://localhost:8080/api/internal/debug'",
                "demo/DebugController.java",
                12,
                RemediationSuggester.suggest("SHADOW_API_EXPOSED", "GET", "/api/internal/debug", null)
        );

        HtmlReportExporter.writeHtmlReport(
                htmlFile.toString(),
                "openapi.json",
                "http://localhost:8080",
                "1.2.0",
                List.of(f1),
                5,
                20,
                true // airGapped = true
        );

        assertTrue(Files.exists(htmlFile), "HTML report should be generated");
        String content = Files.readString(htmlFile);

        assertTrue(content.contains("100% Air-Gapped Mode"),
                "Air-gapped badge should appear in header");
        assertTrue(content.contains("SOC2 Type II &amp; GDPR Compliance Attestation"),
                "Compliance box header should be present");
        assertTrue(content.contains("Zero telemetry/third-party egress"),
                "Air-gapped guarantee text should be present");
        assertTrue(content.contains("Audit SHA-256:"),
                "SHA-256 integrity checksum should be present");
        assertTrue(content.contains("SHADOW_API_EXPOSED"),
                "Finding rule ID should appear in the report");
    }

    @Test
    void testApplyPatchToContent() {
        String originalCode = """
                package com.demo;

                @RestController
                public class DemoController {

                    @GetMapping("/users/{id}")
                    public User getUser(@PathVariable String id) {
                        return userService.find(id);
                    }
                }
                """;

        Finding f = new Finding(
                "STATEFUL_BOLA_CONFIRMED",
                ApiSecurityScanner.Severity.CRITICAL,
                "GET",
                "/users/{id}",
                "path:id",
                "Horizontal privilege escalation confirmed",
                "curl -i -X GET 'http://localhost:8080/users/123'",
                "demo/DemoController.java",
                6,
                RemediationSuggester.suggest("STATEFUL_BOLA_CONFIRMED", "GET", "/users/{id}", null)
        );

        String patched = ApiSecurityScanner.applyPatchToContent(originalCode, f);
        assertNotNull(patched);
        assertTrue(patched.contains("[AUTO-FIX: STATEFUL_BOLA_CONFIRMED]"),
                "Patched content should contain AUTO-FIX comment");
        assertTrue(patched.contains("@PreAuthorize"),
                "Patched content should insert suggested change before target line");
        assertTrue(patched.contains("public User getUser"),
                "Original method should remain in content");
    }
}

