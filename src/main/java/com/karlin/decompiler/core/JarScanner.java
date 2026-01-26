package com.karlin.decompiler.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarScanner {
    public List<JarIndex> scan(Path root) throws IOException {
        List<Path> jarPaths = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                .forEach(jarPaths::add);
        }

        List<JarIndex> indices = new ArrayList<>();
        for (Path jarPath : jarPaths) {
            indices.add(scanJar(jarPath));
        }
        return indices;
    }

    private JarIndex scanJar(Path jarPath) throws IOException {
        Set<String> classNames = new HashSet<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && !name.endsWith("module-info.class")) {
                    String internalName = name.substring(0, name.length() - ".class".length());
                    classNames.add(internalName);
                }
            }
        }
        return new JarIndex(jarPath, classNames);
    }
}
