package com.karlin.decompiler.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IndexBuilder {
    private IndexBuilder() {
    }

    public static IndexModel build(List<JarIndex> jars) {
        Map<String, List<ClassLocation>> classesByInternal = new HashMap<>();
        for (JarIndex jar : jars) {
            for (String internalName : jar.getClassInternalNames()) {
                classesByInternal.computeIfAbsent(internalName, key -> new ArrayList<>())
                    .add(new ClassLocation(jar.getJarPath(), internalName));
            }
        }
        return new IndexModel(jars, classesByInternal);
    }
}
