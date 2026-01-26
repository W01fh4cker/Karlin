package com.karlin.decompiler.engine;

import com.karlin.decompiler.core.JarClassReader;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

public final class VineflowerEngine implements DecompilerEngine {
    @Override
    public String getId() {
        return "fernflower";
    }

    @Override
    public String getDisplayName() {
        return "Fernflower (Vineflower)";
    }

    @Override
    public String decompile(Path jarPath, String internalName, Charset displayCharset) throws Exception {
        String normalized = internalName.replace('.', '/');
        AtomicReference<String> result = new AtomicReference<>();

        Path tempClass = Files.createTempFile("vineflower-", ".class");
        try {
            byte[] bytes = JarClassReader.readClassBytes(jarPath, internalName);
            Files.write(tempClass, bytes);

        IResultSaver saver = new IResultSaver() {
            @Override
            public void saveFolder(String path) {
            }

            @Override
            public void copyFile(String source, String path, String entryName) {
            }

            @Override
            public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
                capture(qualifiedName, entryName, content);
            }

            @Override
            public void createArchive(String path, String archiveName, java.util.jar.Manifest manifest) {
            }

            @Override
            public void saveDirEntry(String path, String archiveName, String entryName) {
            }

            @Override
            public void copyEntry(String source, String path, String archiveName, String entry) {
            }

            @Override
            public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
                capture(qualifiedName, entryName, content);
            }

            @Override
            public void closeArchive(String path, String archiveName) {
            }

            private void capture(String qualifiedName, String entryName, String content) {
                if (result.get() != null) {
                    return;
                }
                String entry = entryName == null ? "" : entryName.replace('\\', '/');
                if (normalized.equals(qualifiedName) || entry.endsWith(normalized + ".java")) {
                    result.set(content);
                }
            }
        };

            Decompiler decompiler = new Decompiler.Builder()
                .inputs(tempClass.toFile())
                .output(saver)
                .logger(IFernflowerLogger.NO_OP)
                .build();

            decompiler.decompile();
        } finally {
            try {
                Files.deleteIfExists(tempClass);
            } catch (Exception ignored) {
                // ignore cleanup errors
            }
        }

        String source = result.get();
        if (source == null) {
            throw new IllegalStateException("Class not found in decompiler output: " + normalized);
        }
        return source;
    }
}
