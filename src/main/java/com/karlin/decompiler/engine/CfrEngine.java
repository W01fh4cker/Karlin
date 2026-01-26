package com.karlin.decompiler.engine;

import com.karlin.decompiler.core.JarClassReader;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.api.OutputSinkFactory.SinkClass;
import org.benf.cfr.reader.api.OutputSinkFactory.SinkType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CfrEngine implements DecompilerEngine {
    @Override
    public String getId() {
        return "cfr";
    }

    @Override
    public String getDisplayName() {
        return "CFR";
    }

    @Override
    public String decompile(Path jarPath, String internalName, Charset displayCharset) throws Exception {
        String tempFile = writeTempClass(jarPath, internalName).toString();
        StringBuilder output = new StringBuilder();

        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, java.util.Collection<SinkClass> available) {
                return List.of(SinkClass.DECOMPILED, SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA) {
                    if (sinkClass == SinkClass.DECOMPILED) {
                        return sinkable -> output.append(((SinkReturns.Decompiled) sinkable).getJava());
                    }
                    return sinkable -> output.append(sinkable.toString());
                }
                return sinkable -> { };
            }
        };

        Map<String, String> options = new HashMap<>();
        options.put("hideutf", "true");
        options.put("outputdir", "");
        options.put("extraclasspath", jarPath.toString());

        CfrDriver driver = new CfrDriver.Builder()
            .withOptions(options)
            .withOutputSink(sinkFactory)
            .build();

        driver.analyse(Collections.singletonList(tempFile));
        deleteTempFile(tempFile);

        return output.toString();
    }

    private Path writeTempClass(Path jarPath, String internalName) throws IOException {
        byte[] bytes = JarClassReader.readClassBytes(jarPath, internalName);
        Path tempFile = Files.createTempFile("cfr-", ".class");
        Files.write(tempFile, bytes);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    private void deleteTempFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ignored) {
            // ignore cleanup errors
        }
    }

    
}
