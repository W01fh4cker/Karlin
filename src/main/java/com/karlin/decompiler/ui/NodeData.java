package com.karlin.decompiler.ui;

import java.nio.file.Path;

public final class NodeData {
    private final NodeType type;
    private final String label;
    private final Path jarPath;
    private final String internalName;
    private final ClassKind classKind;

    public NodeData(NodeType type, String label, Path jarPath, String internalName) {
        this(type, label, jarPath, internalName, null);
    }

    public NodeData(NodeType type, String label, Path jarPath, String internalName, ClassKind classKind) {
        this.type = type;
        this.label = label;
        this.jarPath = jarPath;
        this.internalName = internalName;
        this.classKind = classKind;
    }

    public NodeType getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public Path getJarPath() {
        return jarPath;
    }

    public String getInternalName() {
        return internalName;
    }

    public ClassKind getClassKind() {
        return classKind;
    }

    @Override
    public String toString() {
        return label;
    }
}
