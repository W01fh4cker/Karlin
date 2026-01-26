package com.karlin.decompiler.engine;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

import java.nio.charset.Charset;
import java.nio.file.Path;

public final class JadxEngine implements DecompilerEngine {
    @Override
    public String getId() {
        return "jadx";
    }

    @Override
    public String getDisplayName() {
        return "Jadx";
    }

    @Override
    public String decompile(Path jarPath, String internalName, Charset displayCharset) throws Exception {
        String fullName = internalName.replace('/', '.');
        JadxArgs args = new JadxArgs();
        args.getInputFiles().add(jarPath.toFile());
        args.setSkipResources(true);

        try (JadxDecompiler jadx = new JadxDecompiler(args)) {
            jadx.load();
            JavaClass clazz = jadx.searchJavaClassByOrigFullName(fullName);
            if (clazz == null) {
                throw new IllegalStateException("Class not found by Jadx: " + fullName);
            }
            String source = clazz.getCode();
            return source;
        }
    }
}
