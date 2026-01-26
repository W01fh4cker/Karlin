package com.karlin.decompiler.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceIndexBuilder {
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(class|interface|enum)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b");
    private static final Pattern METHOD_PATTERN = Pattern.compile("^(\\s*(public|protected|private|static|final|synchronized|abstract|native|strictfp)\\s+)*[\\w\\<\\>\\[\\]]+\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^(\\s*(public|protected|private|static|final|transient|volatile)\\s+)*[\\w\\<\\>\\[\\]]+\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b.*;");

    private SourceIndexBuilder() {
    }

    private static String normalizeParams(String params) {
        if (params == null) {
            return "";
        }
        String cleaned = params.replaceAll("<[^>]+>", "");
        cleaned = cleaned.replaceAll("\\bfinal\\b", "");
        cleaned = cleaned.replaceAll("@\\w+", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        String[] parts = cleaned.split(",");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            String[] tokens = p.split(" ");
            if (tokens.length > 0) {
                String type = tokens[0];
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(type);
            }
        }
        return builder.toString();
    }

    public static SourceIndex build(String source) {
        Map<String, Integer> methodLines = new HashMap<>();
        Map<String, Integer> fieldLines = new HashMap<>();
        Map<String, Integer> classLines = new HashMap<>();

        if (source == null || source.isEmpty()) {
            return new SourceIndex(methodLines, fieldLines, classLines);
        }

        String[] lines = source.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;

            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                String name = classMatcher.group(2);
                classLines.putIfAbsent(name, lineNumber);
            }

            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (methodMatcher.find()) {
                String name = methodMatcher.group(3);
                String params = methodMatcher.group(4);
                String key = name + "(" + normalizeParams(params) + ")";
                methodLines.putIfAbsent(key, lineNumber);
                continue;
            }

            Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
            if (fieldMatcher.find()) {
                String name = fieldMatcher.group(3);
                fieldLines.putIfAbsent(name, lineNumber);
            }
        }

        return new SourceIndex(methodLines, fieldLines, classLines);
    }
}
