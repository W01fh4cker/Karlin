package com.karlin.decompiler.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DecompileCache {
    private final Map<CacheKey, String> cache = new ConcurrentHashMap<>();

    public String get(String engineId, Path jarPath, String internalName) {
        return cache.get(new CacheKey(engineId, jarPath, internalName));
    }

    public void put(String engineId, Path jarPath, String internalName, String source) {
        cache.put(new CacheKey(engineId, jarPath, internalName), source);
    }

    public void clear() {
        cache.clear();
    }

    private static final class CacheKey {
        private final String engineId;
        private final Path jarPath;
        private final String internalName;

        private CacheKey(String engineId, Path jarPath, String internalName) {
            this.engineId = engineId;
            this.jarPath = jarPath;
            this.internalName = internalName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CacheKey)) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(engineId, cacheKey.engineId)
                && Objects.equals(jarPath, cacheKey.jarPath)
                && Objects.equals(internalName, cacheKey.internalName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(engineId, jarPath, internalName);
        }
    }
}
