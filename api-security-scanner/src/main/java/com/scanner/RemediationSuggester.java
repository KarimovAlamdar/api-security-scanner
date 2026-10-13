package com.scanner;

/**
 * Generates actionable GitHub PR "Suggested Changes" (Zero-Click Git Patches)
 * with concrete, defensive remediations for mapped vulnerability findings.
 */
public final class RemediationSuggester {

    private RemediationSuggester() {
    }

    /**
     * Produces a code snippet suitable for embedding inside a GitHub
     * ```suggestion markdown block targeting the mapped controller line.
     */
    public static String suggest(String findingId, String method, String path, String location) {
        if (findingId == null) {
            return null;
        }
        return switch (findingId) {
            case "SQLI_INDICATOR" -> """
                // [Remediation: SQL Injection] Use parameterized queries (JPA / PreparedStatement)
                // Never concatenate user-supplied input into database queries.
                @Query("SELECT e FROM Entity e WHERE e.id = :id")
                Optional<Entity> findByIdSecure(@Param("id") Long id);
                // Alternatively with JDBC PreparedStatement:
                // try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
                //     ps.setLong(1, id);
                //     return ps.executeQuery();
                // }""";

            case "AUTH_BYPASS" -> """
                // [Remediation: Auth Bypass] Enforce authentication and role-based access control
                @PreAuthorize("isAuthenticated()")
                // Or restrict to authorized roles:
                // @PreAuthorize("hasRole('USER') or hasAuthority('SCOPE_read')")""";

            case "CORS_WILDCARD" -> """
                // [Remediation: CORS Wildcard] Restrict Access-Control-Allow-Origin to specific trusted domains
                @CrossOrigin(origins = {"https://trusted-app.example.com"}, allowCredentials = "true")""";

            case "CORS_REFLECTED_ORIGIN" -> """
                // [Remediation: CORS Reflected Origin] Validate origin against a strict whitelist
                @CrossOrigin(origins = {"https://trusted-app.example.com", "https://admin.example.com"}, allowCredentials = "true")""";

            case "BOLA_IDOR_VULNERABILITY" -> """
                // [Remediation: BOLA/IDOR] Enforce Broken Object Level Authorization checks
                // Verify that the authenticated user owns or is authorized to access the target object
                @PreAuthorize("@securityService.isResourceOwner(authentication, #id)")""";

            case "ERROR_LEAK" -> """
                // [Remediation: Information Leak] Return sanitized error responses and hide stack traces
                @ExceptionHandler(Exception.class)
                public ResponseEntity<Map<String, String>> handleException(Exception ex) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "An internal error occurred. Please contact support."));
                }""";

            case "SERVER_ERROR" -> """
                // [Remediation: Unhandled Exception] Implement proper input validation and error handling
                if (id == null || id <= 0) {
                    return ResponseEntity.badRequest().body("Invalid identifier specified");
                }""";

            case "XSS_REFLECTED" -> """
                // [Remediation: Reflected XSS] HTML-encode all user input before reflecting in responses.
                // Use a templating engine with auto-escaping (Thymeleaf, Mustache) or encode manually:
                import org.springframework.web.util.HtmlUtils;
                String safeOutput = HtmlUtils.htmlEscape(userInput);
                // For JSON APIs, ensure Content-Type is application/json (not text/html):
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("result", safeOutput));""";

            case "MISSING_HSTS" -> """
                // [Remediation: HSTS] Add Strict-Transport-Security header via Spring Security
                @Bean
                public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                    http.headers(headers -> headers
                            .httpStrictTransportSecurity(hsts -> hsts
                                    .maxAgeInSeconds(31536000)  // 1 year
                                    .includeSubDomains(true)
                                    .preload(true)));
                    return http.build();
                }""";

            case "MISSING_CSP" -> """
                // [Remediation: CSP] Add Content-Security-Policy header
                @Bean
                public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                    http.headers(headers -> headers
                            .contentSecurityPolicy(csp -> csp
                                    .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'")));
                    return http.build();
                }""";

            case "MISSING_XCTO" -> """
                // [Remediation: X-Content-Type-Options] Prevent MIME-type sniffing
                // Spring Security adds this by default when enabled:
                @Bean
                public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                    http.headers(headers -> headers.contentTypeOptions(Customizer.withDefaults()));
                    return http.build();
                }""";

            case "MISSING_XFO" -> """
                // [Remediation: X-Frame-Options] Prevent clickjacking by denying iframe embedding
                @Bean
                public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                    http.headers(headers -> headers.frameOptions(frame -> frame.deny()));
                    return http.build();
                }""";

            case "SERVER_VERSION_LEAK" -> """
                // [Remediation: Server Version Leak] Remove or anonymize the Server header.
                // For Spring Boot with embedded Tomcat:
                server.tomcat.server-header=  # empty in application.properties
                // Or via WebServerFactoryCustomizer:
                @Bean
                public WebServerFactoryCustomizer<TomcatServletWebServerFactory> hideServerHeader() {
                    return factory -> factory.addConnectorCustomizers(
                            connector -> connector.setProperty("server", ""));
                }""";

            case "TECH_STACK_LEAK" -> """
                // [Remediation: X-Powered-By Leak] Remove the X-Powered-By header.
                // For Spring Boot: set server.servlet.header.x-powered-by.enabled=false
                // Or add a response filter:
                response.setHeader("X-Powered-By", null);""";

            case "COOKIE_NO_HTTPONLY" -> """
                // [Remediation: Cookie HttpOnly] Set HttpOnly flag to prevent JavaScript access
                Cookie cookie = new Cookie("session", value);
                cookie.setHttpOnly(true);
                // Or via Spring Session:
                server.servlet.session.cookie.http-only=true""";

            case "COOKIE_NO_SECURE" -> """
                // [Remediation: Cookie Secure] Set Secure flag to prevent plaintext transmission
                Cookie cookie = new Cookie("session", value);
                cookie.setSecure(true);
                // Or via Spring Session:
                server.servlet.session.cookie.secure=true""";

            case "COOKIE_NO_SAMESITE" -> """
                // [Remediation: Cookie SameSite] Set SameSite attribute to prevent CSRF attacks
                // Spring Boot 2.6+:
                server.servlet.session.cookie.same-site=lax
                // Or manually via ResponseCookie:
                ResponseCookie cookie = ResponseCookie.from("session", value)
                        .sameSite("Lax")
                        .httpOnly(true)
                        .secure(true)
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());""";

            case "SSRF_DETECTED", "SSRF_POTENTIAL" -> """
                // [Remediation: SSRF] Restrict outgoing requests to a strict allowlist of domains/IPs.
                // Disallow private IP ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.0/8, 169.254.169.254).
                InetAddress addr = InetAddress.getByName(uri.getHost());
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) {
                    throw new SecurityException("Access to internal/private IP addresses is blocked");
                }
                // Validate against allowlist:
                if (!ALLOWED_HOSTS.contains(uri.getHost().toLowerCase(java.util.Locale.ROOT))) {
                    throw new SecurityException("Destination host is not in the trusted allowlist");
                }""";

            case "PATH_TRAVERSAL" -> """
                // [Remediation: Path Traversal] Normalize and validate file paths against base directory.
                Path baseDir = Path.of("/var/app/data").toAbsolutePath().normalize();
                Path targetPath = baseDir.resolve(userInputFilename).normalize();
                if (!targetPath.startsWith(baseDir)) {
                    throw new SecurityException("Directory traversal attempt detected: " + userInputFilename);
                }""";

            case "SQLI_BLIND_TIME" -> """
                // [Remediation: Blind SQLi] Use parameterized queries or ORM criteria APIs.
                // Never construct dynamic SQL with concatenated user input:
                @Query("SELECT u FROM User u WHERE u.username = :username")
                Optional<User> findByUsername(@Param("username") String username);
                // Or with Spring JdbcTemplate:
                // jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", rowMapper, id);""";

            case "COMMAND_INJECTION" -> """
                // [Remediation: Command Injection] Avoid executing shell commands directly.
                // If system execution is strictly required, use ProcessBuilder with discrete arguments (no shell interpretation):
                ProcessBuilder pb = new ProcessBuilder("convert", "-resize", "100x100", safeFilename, outputFilename);
                pb.redirectErrorStream(true);
                Process process = pb.start();""";

            case "SSTI_DETECTED" -> """
                // [Remediation: SSTI] Never concatenate user input into template source code.
                // Pass user data as context variables into pre-compiled templates:
                Context context = new Context();
                context.setVariable("username", userInput);
                templateEngine.process("user-profile.html", context, writer);""";

            case "OPEN_REDIRECT" -> """
                // [Remediation: Open Redirect] Validate redirect destination against an allowed domains list.
                // Reject absolute external URLs or restrict to relative internal paths:
                URI targetUri = URI.create(redirectUrl);
                if (targetUri.isAbsolute() && !ALLOWED_DOMAINS.contains(targetUri.getHost())) {
                    return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/home")).build();
                }
                return ResponseEntity.status(HttpStatus.SEE_OTHER).location(targetUri).build();""";

            case "NOSQL_INJECTION" -> """
                // [Remediation: NoSQL Injection] Enforce strict DTO schema binding or type validation.
                // Do not pass raw JSON Maps/Objects directly into MongoDB queries:
                Query query = new Query(Criteria.where("username").is(loginRequest.getUsername()));
                User user = mongoTemplate.findOne(query, User.class);""";

            case "SHADOW_API_EXPOSED" -> """
                // [Remediation: Shadow API] Either register this endpoint in the OpenAPI specification
                // or restrict/remove this unmonitored internal controller endpoint:
                // 1. Add route to openapi.json/openapi.yaml or document with @Operation.
                // 2. If intentionally internal, mark with @Hidden or restrict to ADMIN:
                @Hidden
                @PreAuthorize("hasRole('ADMIN')")
                // Or disable in production profiles:
                @Profile("!production")""";

            case "ZOMBIE_API_ENDPOINT" -> """
                // [Remediation: Zombie API] Remove deprecated/abandoned route definition from openapi.json
                // or decommission/mark the endpoint as deprecated in the controller and OpenAPI spec:
                @Deprecated
                @Operation(deprecated = true)
                // In OpenAPI spec:
                // deprecated: true""";

            case "STATEFUL_BOLA_CONFIRMED" -> """
                // [Remediation: Stateful BOLA/IDOR] Verify object ownership before granting access or modification:
                @PreAuthorize("@securityService.isResourceOwner(authentication, #id)")
                // In service layer:
                Resource res = repository.findById(id).orElseThrow(NotFoundException::new);
                if (!res.getOwnerId().equals(currentAuthenticatedUser.getId())) {
                    throw new AccessDeniedException("Access denied to requested resource.");
                }""";

            case "LLM_SYSTEM_PROMPT_LEAK" -> """
                // [Remediation: LLM System Prompt Leak] Enforce strict system prompt isolation and output filtering:
                // 1. Separate user input into discrete messages roles (developer/system vs user).
                // 2. Add an output guardrail to redact system instructions or internal tokens:
                if (response.contains("You are a") || response.contains("System prompt:")) {
                    return "I cannot disclose internal system instructions.";
                }""";

            case "LLM_PROMPT_INJECTION" -> """
                // [Remediation: LLM Prompt Injection] Implement structured prompt guardrails:
                // 1. Validate and sanitize user inputs against prompt injection signatures.
                // 2. Use delimiters to isolate user data:
                String safePrompt = "<user_input>\\n" + sanitize(rawInput) + "\\n</user_input>";
                // 3. Deploy an LLM guardrail classifier (e.g. Llama Guard / NeMo Guardrails) before model execution.""";

            default -> null;
        };
    }
}
