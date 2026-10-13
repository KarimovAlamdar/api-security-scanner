package com.scanner;

import com.scanner.ApiSecurityScanner.Severity;

/**
 * Represents a security finding discovered during OpenAPI endpoint testing.
 * Includes mapped source file path and line number (SAST/DAST hybridity)
 * and suggested code remediations (GitHub PR suggestions).
 */
public record Finding(
        String id,
        Severity severity,
        String method,
        String path,
        String location,
        String detail,
        String curlCmd,
        String sourceFile,
        Integer lineNumber,
        String suggestedChange
) {
    public Finding(String id, Severity severity, String method, String path, String location, String detail, String curlCmd) {
        this(id, severity, method, path, location, detail, curlCmd, null, null, null);
    }

    public Finding withSource(String sourceFile, Integer lineNumber, String suggestedChange) {
        return new Finding(id, severity, method, path, location, detail, curlCmd, sourceFile, lineNumber, suggestedChange);
    }
}
