package com.karlin.decompiler.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class IndexModel {
    private final List<JarIndex> jars;
    private final Map<String, List<ClassLocation>> classesByInternalName;

    public IndexModel(List<JarIndex> jars, Map<String, List<ClassLocation>> classesByInternalName) {
        this.jars = Collections.unmodifiableList(jars);
        this.classesByInternalName = Collections.unmodifiableMap(classesByInternalName);
    }

    public List<JarIndex> getJars() {
        return jars;
    }

    public Map<String, List<ClassLocation>> getClassesByInternalName() {
        return classesByInternalName;
    }
}
