package com.karlin.decompiler.ui;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SourceIndexCache {
    private final Map<Key, SourceIndex> cache = new ConcurrentHashMap<>();

    public void put(String engineId, Path jarPath, String internalName, SourceIndex index) {
        if (index == null) {
            return;
        }
        cache.put(new Key(engineId, jarPath, internalName), index);
    }

    public SourceIndex get(String engineId, Path jarPath, String internalName) {
        return cache.get(new Key(engineId, jarPath, internalName));
    }

    private static final class Key {
        private final String engineId;
        private final Path jarPath;
        private final String internalName;

        private Key(String engineId, Path jarPath, String internalName) {
            this.engineId = engineId;
            this.jarPath = jarPath;
            this.internalName = internalName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key key = (Key) o;
            return Objects.equals(engineId, key.engineId)
                && Objects.equals(jarPath, key.jarPath)
                && Objects.equals(internalName, key.internalName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(engineId, jarPath, internalName);
        }
    }
}
