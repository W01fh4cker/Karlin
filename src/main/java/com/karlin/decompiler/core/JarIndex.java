package com.karlin.decompiler.core;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

public final class JarIndex {
    private final Path jarPath;
    private final Set<String> classInternalNames;

    public JarIndex(Path jarPath, Set<String> classInternalNames) {
        this.jarPath = jarPath;
        this.classInternalNames = Collections.unmodifiableSet(classInternalNames);
    }

    public Path getJarPath() {
        return jarPath;
    }

    public Set<String> getClassInternalNames() {
        return classInternalNames;
    }
}
