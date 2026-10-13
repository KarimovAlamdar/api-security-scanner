package com.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lightweight local codebase scanner (SAST) that inspects Java source files
 * for controller annotations (Spring MVC / Spring Boot, JAX-RS) and routing handlers,
 * mapping OpenAPI endpoints to the exact source file path and line number.
 */
public final class SourceCodeMapper {

    public record SourceLocation(
            String filePath,
            int lineNumber,
            String httpMethod,
            String routePath
    ) {
        public String display() {
            return filePath + ":" + lineNumber;
        }
    }

    private static final Set<String> IGNORED_DIRS = Set.of(
            ".git", "target", "build", ".gradle", ".idea", ".vscode", "node_modules", "vendor"
    );

    // Spring route annotations: @GetMapping, @PostMapping, @PutMapping, @DeleteMapping, @PatchMapping, @RequestMapping
    private static final Pattern SPRING_MAPPING = Pattern.compile(
            "@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\\s*(?:\\(([^)]*)\\))?",
            Pattern.DOTALL
    );

    // JAX-RS annotations: @Path("..."), @GET, @POST, etc.
    private static final Pattern JAXRS_PATH = Pattern.compile(
            "@Path\\s*\\(\\s*\"([^\"]+)\"\\s*\\)"
    );
    private static final Pattern JAXRS_METHOD = Pattern.compile(
            "@(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)\\b"
    );

    // Lightweight routing handlers (e.g. path.equals("/users"), path.startsWith("/users/"))
    private static final Pattern ROUTE_CALL = Pattern.compile(
            "(?:path\\.equals|path\\.startsWith|server\\.createContext|router\\.(?:get|post|put|delete))\\s*\\(\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CLASS_DECL = Pattern.compile(
            "\\b(?:public|protected|private)?\\s*(?:abstract|final)?\\s*(?:class|record|interface)\\s+(\\w+)"
    );

    private final List<SourceLocation> mappedLocations = new ArrayList<>();

    public SourceCodeMapper() {
    }

    public SourceCodeMapper(String rootPath) {
        if (rootPath != null && !rootPath.isBlank()) {
            scanDirectory(Path.of(rootPath));
        }
    }

    public List<SourceLocation> getMappedLocations() {
        return Collections.unmodifiableList(mappedLocations);
    }

    public void scanDirectory(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(this::isCandidateSourceFile)
                    .forEach(this::scanFile);
        } catch (IOException e) {
            System.out.println("  ! Source scan error on " + root + ": " + e.getMessage());
        }
    }

    private boolean isCandidateSourceFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String fileName = path.getFileName().toString();
        if (!fileName.endsWith(".java")) {
            return false;
        }
        for (Path part : path) {
            if (IGNORED_DIRS.contains(part.toString())) {
                return false;
            }
        }
        return true;
    }

    public void scanFile(Path file) {
        try {
            String content = Files.readString(file);
            // Normalize path separators to forward slashes for cross-platform consistency
            String relative = file.toString().replace('\\', '/');
            if (relative.startsWith("./")) {
                relative = relative.substring(2);
            }
            scanSourceContent(relative, content);
        } catch (IOException e) {
            // Ignore unreadable files
        }
    }

    public void scanSourceContent(String relativePath, String content) {
        String[] lines = content.split("\\R", -1);

        // Track class-level path mapping
        String classLevelPath = "";
        int classDeclLine = -1;

        Matcher classMatcher = CLASS_DECL.matcher(content);
        if (classMatcher.find()) {
            int classStart = classMatcher.start();
            classDeclLine = getLineNumber(content, classStart);
            String beforeClass = content.substring(0, classStart);
            Matcher rmClassMatcher = Pattern.compile("@(?:RequestMapping|Path)\\s*(?:\\(([^)]*)\\))?").matcher(beforeClass);
            while (rmClassMatcher.find()) {
                String val = extractPathFromAnnotationParam(rmClassMatcher.group(1));
                if (val != null && !val.isBlank()) {
                    classLevelPath = normalizePath(val);
                }
            }
        }

        // 1. Scan for Spring annotations
        Matcher springMatcher = SPRING_MAPPING.matcher(content);
        while (springMatcher.find()) {
            int line = getLineNumber(content, springMatcher.start());
            String annotationName = springMatcher.group(1);
            String params = springMatcher.group(2);

            // If class-level @RequestMapping, skip adding method endpoint
            if (classDeclLine > 0 && line < classDeclLine && "RequestMapping".equals(annotationName)) {
                continue;
            }

            String method = resolveSpringHttpMethod(annotationName, params);
            String pathPart = extractPathFromAnnotationParam(params);
            String fullPath = combinePaths(classLevelPath, pathPart);

            mappedLocations.add(new SourceLocation(relativePath, line, method, fullPath));
        }

        // 2. Scan for JAX-RS annotations
        Matcher jaxrsPathMatcher = JAXRS_PATH.matcher(content);
        while (jaxrsPathMatcher.find()) {
            int line = getLineNumber(content, jaxrsPathMatcher.start());
            if (classDeclLine > 0 && line < classDeclLine) {
                continue;
            }
            String pathPart = jaxrsPathMatcher.group(1);
            String fullPath = combinePaths(classLevelPath, pathPart);
            String httpMethod = findNearbyJaxRsMethod(lines, line);
            mappedLocations.add(new SourceLocation(relativePath, line, httpMethod, fullPath));
        }

        // 3. Scan for lightweight / demo routing patterns
        Matcher routeMatcher = ROUTE_CALL.matcher(content);
        while (routeMatcher.find()) {
            int line = getLineNumber(content, routeMatcher.start());
            String pathVal = normalizePath(routeMatcher.group(1));
            String httpMethod = findNearbyHttpMethod(lines, line);
            mappedLocations.add(new SourceLocation(relativePath, line, httpMethod, pathVal));
        }
    }

    /**
     * Finds the best matching source location for a given HTTP method and OpenAPI path.
     */
    public SourceLocation findLocation(String method, String openApiPath) {
        if (openApiPath == null || mappedLocations.isEmpty()) {
            return null;
        }

        String targetMethod = method != null ? method.toUpperCase(Locale.ROOT) : "";
        String normalizedTarget = normalizePath(openApiPath);

        SourceLocation bestMatch = null;
        int bestScore = -1;

        for (SourceLocation loc : mappedLocations) {
            int score = scoreMatch(loc, targetMethod, normalizedTarget);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = loc;
            }
        }

        return bestScore >= 50 ? bestMatch : null;
    }

    private int scoreMatch(SourceLocation loc, String targetMethod, String targetPath) {
        String locMethod = loc.httpMethod() != null ? loc.httpMethod().toUpperCase(Locale.ROOT) : "ALL";
        boolean methodMatches = locMethod.equals("ALL") || locMethod.equals(targetMethod);
        if (!methodMatches) {
            return 0;
        }

        String locPath = normalizePath(loc.routePath());
        int methodScore = locMethod.equals(targetMethod) ? 50 : 20;

        // 1. Exact match
        if (locPath.equalsIgnoreCase(targetPath)) {
            return methodScore + 100;
        }

        // 2. Wildcard / template match ({id} vs {userId})
        if (pathTemplatesMatch(locPath, targetPath)) {
            return methodScore + 80;
        }

        // 3. Suffix or prefix match (e.g. context-path /api/users vs /users)
        if (isPrefixOrSuffixMatch(locPath, targetPath)) {
            return methodScore + 40;
        }

        return 0;
    }

    private static boolean pathTemplatesMatch(String p1, String p2) {
        String regex1 = p1.replaceAll("\\{[^}/]+\\}", "[^/]+");
        String regex2 = p2.replaceAll("\\{[^}/]+\\}", "[^/]+");
        if (p2.matches(regex1) || p1.matches(regex2)) {
            return true;
        }
        // Normalize all placeholders to wildcard tokens
        String tokenized1 = p1.replaceAll("\\{[^}/]+\\}", "*");
        String tokenized2 = p2.replaceAll("\\{[^}/]+\\}", "*");
        return tokenized1.equalsIgnoreCase(tokenized2);
    }

    private static boolean isPrefixOrSuffixMatch(String p1, String p2) {
        if (p1.isEmpty() || p2.isEmpty() || p1.equals("/") || p2.equals("/")) {
            return false;
        }
        String clean1 = p1.replaceAll("^/+", "").replaceAll("/+$", "");
        String clean2 = p2.replaceAll("^/+", "").replaceAll("/+$", "");
        return clean1.endsWith(clean2) || clean2.endsWith(clean1)
                || clean1.startsWith(clean2) || clean2.startsWith(clean1);
    }

    static int getLineNumber(String text, int charIndex) {
        int line = 1;
        int max = Math.min(charIndex, text.length());
        for (int i = 0; i < max; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    static String extractPathFromAnnotationParam(String params) {
        if (params == null || params.isBlank()) {
            return "";
        }
        Matcher m = Pattern.compile("(?:value|path)\\s*=\\s*(?:\\{\\s*)?\"([^\"]+)\"|\"([^\"]+)\"").matcher(params);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        return "";
    }

    static String resolveSpringHttpMethod(String annotationName, String params) {
        return switch (annotationName) {
            case "GetMapping" -> "GET";
            case "PostMapping" -> "POST";
            case "PutMapping" -> "PUT";
            case "DeleteMapping" -> "DELETE";
            case "PatchMapping" -> "PATCH";
            case "RequestMapping" -> {
                if (params != null) {
                    Matcher m = Pattern.compile("RequestMethod\\.(GET|POST|PUT|DELETE|PATCH)").matcher(params);
                    if (m.find()) {
                        yield m.group(1);
                    }
                }
                yield "ALL";
            }
            default -> "ALL";
        };
    }

    static String combinePaths(String classPath, String methodPath) {
        String p1 = classPath == null ? "" : classPath.trim();
        String p2 = methodPath == null ? "" : methodPath.trim();
        if (p1.isEmpty() && p2.isEmpty()) {
            return "/";
        }
        String combined = p1 + "/" + p2;
        combined = combined.replaceAll("/+", "/");
        if (combined.length() > 1 && combined.endsWith("/")) {
            combined = combined.substring(0, combined.length() - 1);
        }
        if (!combined.startsWith("/")) {
            combined = "/" + combined;
        }
        return combined;
    }

    static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String p = path.trim().replaceAll("/+", "/");
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private String findNearbyJaxRsMethod(String[] lines, int line) {
        int start = Math.max(0, line - 4);
        int end = Math.min(lines.length - 1, line + 3);
        for (int i = start; i <= end; i++) {
            Matcher m = JAXRS_METHOD.matcher(lines[i]);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "ALL";
    }

    private String findNearbyHttpMethod(String[] lines, int line) {
        int start = Math.max(0, line - 3);
        int end = Math.min(lines.length - 1, line + 5);
        for (int i = start; i <= end; i++) {
            Matcher m = Pattern.compile("method\\.equals\\s*\\(\\s*\"(GET|POST|PUT|DELETE)\"", Pattern.CASE_INSENSITIVE)
                    .matcher(lines[i]);
            if (m.find()) {
                return m.group(1).toUpperCase(Locale.ROOT);
            }
        }
        return "ALL";
    }

    /**
     * Checks if a scanned source location corresponds to any declared OpenAPI path.
     * Used for Shadow API discovery (detecting endpoints that exist in code but are absent from OpenAPI spec).
     */
    public static boolean matchesAnySpecEndpoint(SourceLocation loc, Collection<String> specPaths) {
        if (loc == null || loc.routePath() == null || specPaths == null || specPaths.isEmpty()) {
            return false;
        }
        String locPath = normalizePath(loc.routePath());
        for (String specPath : specPaths) {
            String normSpec = normalizePath(specPath);
            if (locPath.equalsIgnoreCase(normSpec)
                    || pathTemplatesMatch(locPath, normSpec)
                    || isPrefixOrSuffixMatch(locPath, normSpec)) {
                return true;
            }
        }
        return false;
    }
}
