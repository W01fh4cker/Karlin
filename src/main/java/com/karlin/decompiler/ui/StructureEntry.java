package com.karlin.decompiler.ui;

public final class StructureEntry {
    private final String kind;
    private final String name;
    private final String descriptor;
    private final Integer lineNumber;

    public StructureEntry(String kind, String name, String descriptor, Integer lineNumber) {
        this.kind = kind;
        this.name = name;
        this.descriptor = descriptor;
        this.lineNumber = lineNumber;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }
}
