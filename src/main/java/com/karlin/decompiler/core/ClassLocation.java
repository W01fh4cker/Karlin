package com.karlin.decompiler.core;

import java.nio.file.Path;
import java.util.Objects;

public final class ClassLocation {
    private final Path jarPath;
    private final String internalName;

    public ClassLocation(Path jarPath, String internalName) {
        this.jarPath = jarPath;
        this.internalName = internalName;
    }

    public Path getJarPath() {
        return jarPath;
    }

    public String getInternalName() {
        return internalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassLocation)) {
            return false;
        }
        ClassLocation that = (ClassLocation) o;
        return Objects.equals(jarPath, that.jarPath) && Objects.equals(internalName, that.internalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jarPath, internalName);
    }
}
