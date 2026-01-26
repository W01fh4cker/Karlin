package com.karlin.decompiler.ui;

import java.nio.file.Path;

public final class SearchResult {
    private final String type;
    private final Path jarPath;
    private final String internalName;
    private final String memberOrPath;
    private final String detail;
    private final Integer lineNumber;

    public SearchResult(String type,
                        Path jarPath,
                        String internalName,
                        String memberOrPath,
                        String detail,
                        Integer lineNumber) {
        this.type = type;
        this.jarPath = jarPath;
        this.internalName = internalName;
        this.memberOrPath = memberOrPath;
        this.detail = detail;
        this.lineNumber = lineNumber;
    }

    public String getType() {
        return type;
    }

    public Path getJarPath() {
        return jarPath;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getMemberOrPath() {
        return memberOrPath;
    }

    public String getDetail() {
        return detail;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }
}
