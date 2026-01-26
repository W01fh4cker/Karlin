package com.karlin.decompiler.engine;

import com.karlin.decompiler.core.JarClassReader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class ProcyonEngine implements DecompilerEngine {
    @Override
    public String getId() {
        return "procyon";
    }

    @Override
    public String getDisplayName() {
        return "Procyon";
    }

    @Override
    public String decompile(Path jarPath, String internalName, Charset displayCharset) throws Exception {
        DecompilerSettings settings = DecompilerSettings.javaDefaults();
        settings.setTypeLoader(new JarTypeLoader(jarPath));

        StringWriter writer = new StringWriter();
        PlainTextOutput output = new PlainTextOutput(writer);
        Decompiler.decompile(internalName, output, settings);
        return writer.toString();
    }

    private static final class JarTypeLoader implements ITypeLoader {
        private final Path jarPath;

        private JarTypeLoader(Path jarPath) {
            this.jarPath = jarPath;
        }

        @Override
        public boolean tryLoadType(String internalName, Buffer buffer) {
            try {
                byte[] bytes = JarClassReader.readClassBytes(jarPath, internalName);
                buffer.reset(bytes.length);
                buffer.putByteArray(bytes, 0, bytes.length);
                buffer.position(0);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    
}
