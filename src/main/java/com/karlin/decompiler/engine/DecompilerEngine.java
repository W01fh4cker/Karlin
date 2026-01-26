package com.karlin.decompiler.engine;

import java.nio.charset.Charset;
import java.nio.file.Path;

public interface DecompilerEngine {
    String getId();

    String getDisplayName();

    String decompile(Path jarPath, String internalName, Charset displayCharset) throws Exception;
}
