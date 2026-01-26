package com.karlin.decompiler.ui;

import com.karlin.decompiler.core.ClassFileInfoReader;
import com.karlin.decompiler.core.ClassLocation;
import com.karlin.decompiler.core.IndexModel;
import com.karlin.decompiler.core.JarIndex;
import com.karlin.decompiler.core.JarClassReader;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class TreeBuilder {
    public DefaultMutableTreeNode buildByJar(IndexModel model) {
        return buildByJar(model, null);
    }

    public DefaultMutableTreeNode buildByJar(IndexModel model, ProgressListener listener) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeData(NodeType.ROOT, "JARs", null, null));
        int total = model.getJars().stream().mapToInt(jar -> jar.getClassInternalNames().size()).sum();
        int[] done = new int[]{0};

        for (JarIndex jar : model.getJars().stream()
            .sorted(Comparator.comparing(j -> j.getJarPath().toString()))
            .toList()) {
            Path jarPath = jar.getJarPath();
            DefaultMutableTreeNode jarNode = new DefaultMutableTreeNode(new NodeData(NodeType.JAR, jarPath.getFileName().toString(), jarPath, null));
            root.add(jarNode);

            Map<String, DefaultMutableTreeNode> packageNodes = new TreeMap<>();
            for (String internalName : jar.getClassInternalNames()) {
                String packageName = packageOf(internalName);
                DefaultMutableTreeNode pkgNode = packageNodes.computeIfAbsent(packageName,
                    key -> createPackageNodes(jarNode, packageName));

                String simpleName = simpleNameOf(internalName);
                ClassKind kind = resolveClassKind(jarPath, internalName);
                DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(
                    new NodeData(NodeType.CLASS, simpleName, jarPath, internalName, kind));
                pkgNode.add(classNode);
                if (listener != null) {
                    done[0]++;
                    if (done[0] % 50 == 0 || done[0] == total) {
                        listener.onProgress(done[0], total);
                    }
                }
            }
        }
        return root;
    }

    public DefaultMutableTreeNode buildByClass(IndexModel model) {
        return buildByClass(model, null);
    }

    public DefaultMutableTreeNode buildByClass(IndexModel model, ProgressListener listener) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeData(NodeType.ROOT, "Classes", null, null));

        Map<String, DefaultMutableTreeNode> packageNodes = new TreeMap<>();
        Map<String, List<ClassLocation>> sorted = new TreeMap<>(model.getClassesByInternalName());
        int total = sorted.size();
        int done = 0;
        for (Map.Entry<String, List<ClassLocation>> entry : sorted.entrySet()) {
            String internalName = entry.getKey();
            List<ClassLocation> locations = entry.getValue();
            String packageName = packageOf(internalName);

            DefaultMutableTreeNode pkgNode = packageNodes.computeIfAbsent(packageName,
                key -> createPackageNodes(root, packageName));

            String simpleName = simpleNameOf(internalName);
            if (locations.size() == 1) {
                ClassLocation location = locations.get(0);
                ClassKind kind = resolveClassKind(location.getJarPath(), internalName);
                DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(
                    new NodeData(NodeType.CLASS, simpleName, location.getJarPath(), internalName, kind));
                pkgNode.add(classNode);
            } else {
                ClassKind kind = resolveClassKind(locations.get(0).getJarPath(), internalName);
                DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(
                    new NodeData(NodeType.CLASS, simpleName, null, internalName, kind));
                pkgNode.add(classNode);
                for (ClassLocation location : locations) {
                    String jarLabel = location.getJarPath().getFileName().toString();
                    DefaultMutableTreeNode jarNode = new DefaultMutableTreeNode(
                        new NodeData(NodeType.CLASS_IN_JAR, jarLabel, location.getJarPath(), internalName, kind));
                    classNode.add(jarNode);
                }
            }
            if (listener != null) {
                done++;
                if (done % 50 == 0 || done == total) {
                    listener.onProgress(done, total);
                }
            }
        }
        return root;
    }

    private ClassKind resolveClassKind(Path jarPath, String internalName) {
        try {
            byte[] bytes = JarClassReader.readClassBytes(jarPath, internalName);
            int accessFlags = ClassFileInfoReader.readAccessFlags(bytes);
            if (ClassFileInfoReader.isEnum(accessFlags)) {
                return ClassKind.ENUM;
            }
            if (ClassFileInfoReader.isInterface(accessFlags)) {
                return ClassKind.INTERFACE;
            }
            return ClassKind.CLASS;
        } catch (Exception e) {
            return ClassKind.CLASS;
        }
    }

    private DefaultMutableTreeNode createPackageNodes(DefaultMutableTreeNode parent, String packageName) {
        if (packageName.isEmpty()) {
            return parent;
        }
        String[] parts = packageName.split("\\.");
        DefaultMutableTreeNode current = parent;
        for (String part : parts) {
            DefaultMutableTreeNode child = null;
            for (int i = 0; i < current.getChildCount(); i++) {
                DefaultMutableTreeNode existing = (DefaultMutableTreeNode) current.getChildAt(i);
                Object userObject = existing.getUserObject();
                if (userObject instanceof NodeData data && data.getLabel().equals(part) && data.getType() == NodeType.PACKAGE) {
                    child = existing;
                    break;
                }
            }
            if (child == null) {
                child = new DefaultMutableTreeNode(new NodeData(NodeType.PACKAGE, part, null, null));
                current.add(child);
            }
            current = child;
        }
        return current;
    }

    private String packageOf(String internalName) {
        int lastSlash = internalName.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "";
        }
        return internalName.substring(0, lastSlash).replace('/', '.');
    }

    private String simpleNameOf(String internalName) {
        int lastSlash = internalName.lastIndexOf('/');
        return lastSlash >= 0 ? internalName.substring(lastSlash + 1) : internalName;
    }
}
