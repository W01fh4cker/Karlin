package com.karlin.decompiler.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarClassReader {
    private JarClassReader() {
    }

    public static byte[] readClassBytes(Path jarPath, String internalName) throws IOException {
        String entryName = internalName + ".class";
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                throw new IOException("Class not found in jar: " + entryName);
            }
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                return inputStream.readAllBytes();
            }
        }
    }
}
