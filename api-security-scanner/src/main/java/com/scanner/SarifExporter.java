package com.scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates valid SARIF 2.1.0 (Static Analysis Results Interchange Format) JSON reports
 * for direct upload to GitHub's native "Security -> Code scanning alerts" tab
 * via github/codeql-action/upload-sarif with zero external dashboard signups.
 */
public final class SarifExporter {

    private static final String SARIF_SCHEMA = "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json";
    private static final String SARIF_VERSION = "2.1.0";

    private SarifExporter() {
    }

    public static void writeSarif(
            String sarifPath,
            String specPath,
            String baseUrl,
            String toolVersion,
            List<Finding> findings
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("$schema", SARIF_SCHEMA);
        root.put("version", SARIF_VERSION);

        List<Map<String, Object>> runs = new ArrayList<>();
        Map<String, Object> run = new LinkedHashMap<>();

        // Driver / Tool definition
        Map<String, Object> tool = new LinkedHashMap<>();
        Map<String, Object> driver = new LinkedHashMap<>();
        driver.put("name", "api-security-scanner");
        driver.put("semanticVersion", toolVersion);
        driver.put("informationUri", "https://github.com/scanner/api-security-scanner");

        List<Map<String, Object>> rules = buildRules();
        driver.put("rules", rules);
        tool.put("driver", driver);
        run.put("tool", tool);

        Map<String, Integer> ruleIndexMap = new HashMap<>();
        for (int i = 0; i < rules.size(); i++) {
            ruleIndexMap.put((String) rules.get(i).get("id"), i);
        }

        // Build results
        List<Map<String, Object>> results = new ArrayList<>();
        for (Finding f : findings) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ruleId", f.id());
            int ruleIdx = ruleIndexMap.getOrDefault(f.id(), 0);
            result.put("ruleIndex", ruleIdx);

            String level = switch (f.severity()) {
                case CRITICAL, HIGH -> "error";
                case MEDIUM -> "warning";
                case LOW, INFO -> "note";
            };
            result.put("level", level);

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("text", String.format("[%s %s] %s (repro: %s)", f.method(), f.path(), f.detail(), f.curlCmd()));
            result.put("message", message);

            // Mapped physical location in the repository
            String artifactUri = f.sourceFile() != null ? f.sourceFile().replace('\\', '/') : specPath.replace('\\', '/');
            if (artifactUri.startsWith("./")) {
                artifactUri = artifactUri.substring(2);
            }
            int startLine = f.lineNumber() != null && f.lineNumber() > 0 ? f.lineNumber() : 1;

            Map<String, Object> location = new LinkedHashMap<>();
            Map<String, Object> physicalLocation = new LinkedHashMap<>();
            Map<String, Object> artifactLocation = new LinkedHashMap<>();
            artifactLocation.put("uri", artifactUri);
            artifactLocation.put("uriBaseId", "%SRCROOT%");
            physicalLocation.put("artifactLocation", artifactLocation);

            Map<String, Object> region = new LinkedHashMap<>();
            region.put("startLine", startLine);
            region.put("startColumn", 1);
            physicalLocation.put("region", region);

            location.put("physicalLocation", physicalLocation);
            result.put("locations", List.of(location));

            // SARIF code fix suggestion
            if (f.suggestedChange() != null) {
                Map<String, Object> fix = new LinkedHashMap<>();
                fix.put("description", Map.of("text", "Apply suggested security remediation patch"));

                Map<String, Object> fileChange = new LinkedHashMap<>();
                fileChange.put("artifactLocation", artifactLocation);

                Map<String, Object> replacement = new LinkedHashMap<>();
                Map<String, Object> deletedRegion = new LinkedHashMap<>();
                deletedRegion.put("startLine", startLine);
                deletedRegion.put("startColumn", 1);
                replacement.put("deletedRegion", deletedRegion);
                replacement.put("insertedContent", Map.of("text", f.suggestedChange() + "\n"));

                fileChange.put("replacements", List.of(replacement));
                fix.put("fileChanges", List.of(fileChange));
                result.put("fixes", List.of(fix));
            }

            results.add(result);
        }

        run.put("results", results);
        runs.add(run);
        root.put("runs", runs);

        Path out = Path.of(sarifPath);
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), root);
    }

    private static List<Map<String, Object>> buildRules() {
        return List.of(
                rule("BOLA_IDOR_VULNERABILITY", "BrokenObjectLevelAuthorization", "error", "9.5",
                        "Broken Object Level Authorization (BOLA / IDOR)",
                        "Cross-tenant or secondary user accessed or modified another user's private object without authorization check.",
                        List.of("security", "owasp-api1", "CWE-639", "CWE-285")),

                rule("SQLI_INDICATOR", "SqlInjectionIndicator", "error", "9.0",
                        "Database syntax error suggests potential SQL Injection",
                        "Fuzzing payload triggered database syntax error or SQL exception in server response.",
                        List.of("security", "owasp-api8", "CWE-89")),

                rule("AUTH_BYPASS", "AuthenticationBypass", "error", "8.5",
                        "Endpoint missing authentication requirement",
                        "Secured or state-changing endpoint accepted a request without valid authentication credentials.",
                        List.of("security", "owasp-api2", "CWE-306")),

                rule("XSS_REFLECTED", "ReflectedCrossSiteScripting", "error", "8.5",
                        "Reflected Cross-Site Scripting (XSS)",
                        "Input payload was echoed unencoded in the response body, enabling script injection in user browsers.",
                        List.of("security", "owasp-api8", "CWE-79")),

                rule("CORS_REFLECTED_ORIGIN", "CorsReflectedOrigin", "error", "8.0",
                        "CORS reflects arbitrary origin with credentials",
                        "Endpoint reflects arbitrary Origin header while setting Access-Control-Allow-Credentials: true.",
                        List.of("security", "owasp-api7", "CWE-942")),

                rule("CORS_WILDCARD", "CorsWildcardOrigin", "warning", "6.0",
                        "CORS wildcard origin allows cross-origin reading",
                        "Endpoint returns Access-Control-Allow-Origin: * allowing untrusted third-party origins to access API.",
                        List.of("security", "owasp-api7", "CWE-942")),

                rule("MISSING_HSTS", "MissingStrictTransportSecurity", "warning", "5.5",
                        "Missing or weak Strict-Transport-Security (HSTS) header",
                        "Response does not include HSTS or max-age is below recommended 6-month minimum, allowing protocol downgrade attacks.",
                        List.of("security", "owasp-api7", "CWE-319")),

                rule("COOKIE_NO_HTTPONLY", "CookieMissingHttpOnly", "warning", "5.5",
                        "Cookie missing HttpOnly flag",
                        "Set-Cookie header does not include HttpOnly attribute, making cookie accessible to JavaScript and vulnerable to XSS theft.",
                        List.of("security", "owasp-api8", "CWE-1004")),

                rule("COOKIE_NO_SECURE", "CookieMissingSecure", "warning", "5.0",
                        "Cookie missing Secure flag on HTTPS endpoint",
                        "Set-Cookie header on an HTTPS endpoint does not include Secure attribute, allowing cookie transmission over plaintext HTTP.",
                        List.of("security", "owasp-api8", "CWE-614")),

                rule("ERROR_LEAK", "InternalErrorLeak", "warning", "5.0",
                        "Endpoint leaks internal stack trace or framework error details",
                        "Server returned stack trace or internal error messages containing implementation details.",
                        List.of("security", "CWE-209")),

                rule("MISSING_CSP", "MissingContentSecurityPolicy", "note", "4.0",
                        "Missing Content-Security-Policy header",
                        "Response does not include Content-Security-Policy header, leaving clients vulnerable to content injection attacks.",
                        List.of("security", "owasp-api7", "CWE-693")),

                rule("MISSING_XCTO", "MissingXContentTypeOptions", "note", "3.5",
                        "Missing or incorrect X-Content-Type-Options header",
                        "Response does not include X-Content-Type-Options: nosniff, allowing MIME-type sniffing attacks.",
                        List.of("security", "owasp-api7", "CWE-693")),

                rule("MISSING_XFO", "MissingXFrameOptions", "note", "3.5",
                        "Missing X-Frame-Options / CSP frame-ancestors",
                        "Response lacks both X-Frame-Options header and CSP frame-ancestors directive, enabling clickjacking attacks.",
                        List.of("security", "owasp-api7", "CWE-1021")),

                rule("COOKIE_NO_SAMESITE", "CookieMissingSameSite", "note", "3.5",
                        "Cookie missing SameSite attribute",
                        "Set-Cookie header does not include SameSite attribute (or uses SameSite=None without Secure), enabling CSRF attacks.",
                        List.of("security", "owasp-api8", "CWE-1275")),

                rule("SERVER_VERSION_LEAK", "ServerVersionDisclosure", "note", "3.0",
                        "Server header discloses version information",
                        "The Server response header reveals software name and version, aiding attackers in identifying known vulnerabilities.",
                        List.of("security", "CWE-200")),

                rule("TECH_STACK_LEAK", "TechnologyStackDisclosure", "note", "3.0",
                        "X-Powered-By header leaks technology stack",
                        "The X-Powered-By header reveals framework or runtime details, aiding targeted exploitation.",
                        List.of("security", "CWE-200")),

                rule("SSRF_DETECTED", "ServerSideRequestForgery", "error", "9.5",
                        "Server-Side Request Forgery (SSRF) to cloud metadata or internal host",
                        "Endpoint forwarded request to internal cloud metadata service (169.254.169.254) or localhost and returned internal metadata or service banners.",
                        List.of("security", "owasp-api7", "CWE-918")),

                rule("SSRF_POTENTIAL", "PotentialServerSideRequestForgery", "warning", "7.5",
                        "Potential Server-Side Request Forgery",
                        "Endpoint accepted an internal URL payload and responded with HTTP 200, suggesting lack of destination IP/hostname validation.",
                        List.of("security", "owasp-api7", "CWE-918")),

                rule("PATH_TRAVERSAL", "PathTraversalArbitraryFileRead", "error", "9.0",
                        "Path Traversal / Arbitrary File Read",
                        "File system path traversal sequence escaped root folder and returned sensitive system configuration files.",
                        List.of("security", "owasp-api8", "CWE-22")),

                rule("SQLI_BLIND_TIME", "TimeBasedBlindSqlInjection", "error", "9.2",
                        "Time-based Blind SQL Injection",
                        "Injected SQL delay payload caused significant server response latency matching the injected sleep duration.",
                        List.of("security", "owasp-api8", "CWE-89")),

                rule("COMMAND_INJECTION", "RemoteOsCommandInjection", "error", "9.8",
                        "Remote OS Command Injection",
                        "Injected shell command / delay payload triggered remote operating system command execution.",
                        List.of("security", "owasp-api8", "CWE-78")),

                rule("SSTI_DETECTED", "ServerSideTemplateInjection", "error", "8.5",
                        "Server-Side Template Injection (SSTI)",
                        "Template expression evaluation payload was computed and reflected by the server template engine.",
                        List.of("security", "owasp-api8", "CWE-1336")),

                rule("OPEN_REDIRECT", "OpenRedirect", "warning", "6.5",
                        "Open URL Redirection",
                        "Endpoint issues HTTP 3xx redirect to untrusted external URL from user-controlled parameter.",
                        List.of("security", "owasp-api7", "CWE-601")),

                rule("NOSQL_INJECTION", "NoSqlInjection", "error", "8.5",
                        "NoSQL Operator Injection",
                        "MongoDB/NoSQL query operator payload altered database query logic or bypassed filtering.",
                        List.of("security", "owasp-api8", "CWE-943")),

                rule("SHADOW_API_EXPOSED", "ShadowApiExposed", "error", "8.5",
                        "Shadow / Undocumented API Endpoint Exposed in Source Code",
                        "Endpoint exists in source code controllers but is completely omitted from the OpenAPI specification, exposing unmonitored attack surface.",
                        List.of("security", "owasp-api9", "CWE-1059", "shadow-api")),

                rule("ZOMBIE_API_ENDPOINT", "ZombieApiEndpoint", "note", "3.0",
                        "Zombie / Deprecated API Endpoint Missing in Codebase",
                        "Endpoint is declared in the OpenAPI specification but could not be located in source code controllers, indicating dead code or specification drift.",
                        List.of("security", "owasp-api9", "CWE-1059", "zombie-api")),

                rule("STATEFUL_BOLA_CONFIRMED", "StatefulBolaConfirmed", "error", "10.0",
                        "Stateful BOLA / IDOR Confirmed Via Dynamic Resource Creation",
                        "User A dynamically created an entity, and secondary user (User B) successfully accessed or manipulated it with zero false positive verification.",
                        List.of("security", "owasp-api1", "CWE-639", "CWE-285", "stateful-bola")),

                rule("LLM_SYSTEM_PROMPT_LEAK", "LlmSystemPromptLeak", "error", "8.5",
                        "LLM System Prompt / Sensitive Pre-Prompt Leakage",
                        "Target endpoint processed an injection prompt and leaked internal system directives, safety guardrails, or hidden developer instructions.",
                        List.of("security", "owasp-llm06", "CWE-200", "ai-security")),

                rule("LLM_PROMPT_INJECTION", "LlmPromptInjection", "error", "8.8",
                        "LLM Prompt Injection / Guardrail Compromised",
                        "Direct prompt injection successfully bypassed LLM safety guardrails and executed adversarial instructions.",
                        List.of("security", "owasp-llm01", "CWE-94", "ai-security")),

                rule("SERVER_ERROR", "UnhandledServerError", "note", "3.0",
                        "Payload triggered unexpected HTTP 5xx error",
                        "Endpoint returned an internal server error response when processing edge-case input payload.",
                        List.of("security", "CWE-398"))
        );
    }

    private static Map<String, Object> rule(
            String id, String name, String defaultLevel, String securitySeverity,
            String shortDesc, String fullDesc, List<String> tags
    ) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("name", name);
        r.put("shortDescription", Map.of("text", shortDesc));
        r.put("fullDescription", Map.of("text", fullDesc));
        r.put("defaultConfiguration", Map.of("level", defaultLevel));
        r.put("help", Map.of(
                "text", fullDesc,
                "markdown", "**" + shortDesc + "**\n\n" + fullDesc
        ));
        r.put("properties", Map.of(
                "tags", tags,
                "precision", "high",
                "security-severity", securitySeverity
        ));
        return r;
    }
}
