package com.karlin.decompiler.engine;

import com.karlin.decompiler.core.JarClassReader;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class JdCoreEngine implements DecompilerEngine {
    @Override
    public String getId() {
        return "jd-core";
    }

    @Override
    public String getDisplayName() {
        return "JD-Core";
    }

    @Override
    public String decompile(Path jarPath, String internalName, Charset displayCharset) throws Exception {
        ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
        JarLoader loader = new JarLoader(jarPath);
        StringBuilderPrinter printer = new StringBuilderPrinter();
        decompiler.decompile(loader, printer, internalName);
        return printer.toString();
    }

    private static final class JarLoader implements Loader {
        private final Path jarPath;

        private JarLoader(Path jarPath) {
            this.jarPath = jarPath;
        }

        @Override
        public boolean canLoad(String internalName) {
            return true;
        }

        @Override
        public byte[] load(String internalName) throws IOException {
            try {
                return JarClassReader.readClassBytes(jarPath, internalName);
            } catch (Exception e) {
                throw new IOException("Failed to load class: " + internalName, e);
            }
        }
    }

    private static final class StringBuilderPrinter implements Printer {
        private final StringBuilder builder = new StringBuilder();
        private int indent = 0;

        @Override
        public void start(int maxLineNumber, int majorVersion, int minorVersion) {
        }

        @Override
        public void end() {
        }

        @Override
        public void printText(String text) {
            builder.append(text);
        }

        @Override
        public void printNumericConstant(String constant) {
            builder.append(constant);
        }

        @Override
        public void printStringConstant(String constant, String ownerInternalName) {
            builder.append(constant);
        }

        @Override
        public void printKeyword(String keyword) {
            builder.append(keyword);
        }

        @Override
        public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
            builder.append(name);
        }

        @Override
        public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
            builder.append(name);
        }

        @Override
        public void indent() {
            indent++;
        }

        @Override
        public void unindent() {
            indent = Math.max(0, indent - 1);
        }

        @Override
        public void startLine(int lineNumber) {
            builder.append("  ".repeat(Math.max(0, indent)));
        }

        @Override
        public void endLine() {
            builder.append('\n');
        }

        @Override
        public void extraLine(int count) {
            for (int i = 0; i < count; i++) {
                builder.append('\n');
            }
        }

        @Override
        public void startMarker(int type) {
        }

        @Override
        public void endMarker(int type) {
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

    
}
