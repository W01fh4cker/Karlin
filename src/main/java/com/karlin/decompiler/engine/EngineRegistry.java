package com.karlin.decompiler.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EngineRegistry {
    private final List<DecompilerEngine> engines;

    public EngineRegistry() {
        List<DecompilerEngine> list = new ArrayList<>();
        list.add(new VineflowerEngine());
        list.add(new CfrEngine());
        list.add(new ProcyonEngine());
        list.add(new JdCoreEngine());
        list.add(new JadxEngine());
        engines = Collections.unmodifiableList(list);
    }

    public List<DecompilerEngine> getEngines() {
        return engines;
    }
}
