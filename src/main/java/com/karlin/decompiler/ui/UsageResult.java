package com.karlin.decompiler.ui;

import java.nio.file.Path;

public final class UsageResult {
    private final Path jarPath;
    private final String internalName;
    private final Integer lineNumber;
    private final String kind;
    private final String token;

    public UsageResult(Path jarPath, String internalName, Integer lineNumber, String kind, String token) {
        this.jarPath = jarPath;
        this.internalName = internalName;
        this.lineNumber = lineNumber;
        this.kind = kind;
        this.token = token;
    }

    public Path getJarPath() {
        return jarPath;
    }

    public String getInternalName() {
        return internalName;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getKind() {
        return kind;
    }

    public String getToken() {
        return token;
    }
}
