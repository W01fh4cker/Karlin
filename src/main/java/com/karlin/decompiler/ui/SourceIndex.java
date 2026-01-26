package com.karlin.decompiler.ui;

import java.util.Map;

public final class SourceIndex {
    private final Map<String, Integer> methodLines;
    private final Map<String, Integer> fieldLines;
    private final Map<String, Integer> classLines;

    public SourceIndex(Map<String, Integer> methodLines,
                       Map<String, Integer> fieldLines,
                       Map<String, Integer> classLines) {
        this.methodLines = methodLines;
        this.fieldLines = fieldLines;
        this.classLines = classLines;
    }

    public Integer findLine(String kind, String name) {
        if (kind == null || name == null) {
            return null;
        }
        return switch (kind) {
            case "Method" -> methodLines.get(name);
            case "Field" -> fieldLines.get(name);
            case "Class" -> classLines.get(name);
            default -> null;
        };
    }
}
