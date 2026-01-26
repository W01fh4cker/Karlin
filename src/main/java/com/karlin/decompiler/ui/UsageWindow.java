package com.karlin.decompiler.ui;

import com.karlin.decompiler.core.DecompileExecutor;
import com.karlin.decompiler.core.IndexModel;
import com.karlin.decompiler.core.JarClassReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

public final class UsageWindow extends JDialog {
    private final IndexModel indexModel;
    private final DecompileExecutor executor;
    private final UsageKind kind;
    private final String ownerInternal;
    private final String memberName;

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Caller Class", "Caller Method", "Line", "Target"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("Searching...");
    private final List<UsageResult> results = new ArrayList<>();
    private final java.util.function.Consumer<UsageResult> onOpen;

    public UsageWindow(JFrame owner, IndexModel indexModel, DecompileExecutor executor,
                       UsageKind kind, String ownerInternal, String memberName,
                       java.util.function.Consumer<UsageResult> onOpen) {
        super(owner, "Find Usages", ModalityType.MODELESS);
        this.indexModel = indexModel;
        this.executor = executor;
        this.kind = kind;
        this.ownerInternal = ownerInternal;
        this.memberName = memberName;
        this.onOpen = onOpen;

        setSize(900, 500);
        setMinimumSize(new java.awt.Dimension(700, 400));
        setResizable(true);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        initLayout();
        runSearch();
    }

    private void initLayout() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = table.getSelectedRow();
                    if (row >= 0 && row < results.size() && onOpen != null) {
                        onOpen.accept(results.get(row));
                    }
                }
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void runSearch() {
        executor.submit(() -> {
            List<Object[]> rows = new ArrayList<>();
            List<UsageResult> resultList = new ArrayList<>();
            for (var jar : indexModel.getJars()) {
                for (String internalName : jar.getClassInternalNames()) {
                    try {
                        byte[] bytes = JarClassReader.readClassBytes(jar.getJarPath(), internalName);
                        ClassReader reader = new ClassReader(bytes);
                        reader.accept(new UsageClassVisitor(rows, resultList, jar.getJarPath(), internalName,
                            kind, ownerInternal, memberName), ClassReader.SKIP_FRAMES);
                    } catch (Exception ignored) {
                    }
                }
            }
            SwingUtilities.invokeLater(() -> {
                for (Object[] row : rows) {
                    tableModel.addRow(row);
                }
                results.clear();
                results.addAll(resultList);
                statusLabel.setText("Results: " + rows.size());
            });
            return null;
        });
    }

    private static final class UsageClassVisitor extends ClassVisitor {
        private final List<Object[]> rows;
        private final List<UsageResult> resultList;
        private final java.nio.file.Path jarPath;
        private final String internalName;
        private final UsageKind kind;
        private final String ownerInternal;
        private final String memberName;
        private String className;

        private UsageClassVisitor(List<Object[]> rows,
                                  List<UsageResult> resultList,
                                  java.nio.file.Path jarPath,
                                  String internalName,
                                  UsageKind kind,
                                  String ownerInternal,
                                  String memberName) {
            super(Opcodes.ASM9);
            this.rows = rows;
            this.resultList = resultList;
            this.jarPath = jarPath;
            this.internalName = internalName;
            this.kind = kind;
            this.ownerInternal = ownerInternal;
            this.memberName = memberName;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name.replace('/', '.');
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                private int currentLine = -1;

                @Override
                public void visitLineNumber(int line, org.objectweb.asm.Label start) {
                    currentLine = line;
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (kind != UsageKind.METHOD) {
                        if (kind == UsageKind.CLASS && ownerInternal != null && ownerInternal.equals(owner)) {
                            String target = owner.replace('/', '.') + "." + name + descriptor;
                            addRow(target);
                        }
                        return;
                    }
                    String targetKey = name + "(" + parseDescriptorParams(descriptor) + ")";
                    if (!memberName.equals(targetKey)) {
                        return;
                    }
                    if (ownerInternal != null && !ownerInternal.equals(owner)) {
                        return;
                    }
                    String target = owner.replace('/', '.') + "." + name + descriptor;
                    addRow(target);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    if (kind == UsageKind.FIELD) {
                        if (!memberName.equals(name)) {
                            return;
                        }
                    if (ownerInternal != null && !ownerInternal.equals(owner)) {
                        return;
                    }
                    String target = owner.replace('/', '.') + "." + name + descriptor;
                    addRow(target);
                    return;
                }
                if (kind == UsageKind.CLASS && ownerInternal != null && ownerInternal.equals(owner)) {
                    String target = owner.replace('/', '.') + "." + name + descriptor;
                    addRow(target);
                }
            }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    if (kind == UsageKind.CLASS && ownerInternal != null && ownerInternal.equals(type)) {
                        String target = type.replace('/', '.');
                        addRow(target);
                    }
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (kind == UsageKind.CLASS && value instanceof org.objectweb.asm.Type t) {
                        if (ownerInternal != null && ownerInternal.equals(t.getInternalName())) {
                            String target = t.getClassName();
                            addRow(target);
                        }
                    }
                }

                private String thisMethodSignature() {
                    return name + descriptor;
                }

                private void addRow(String target) {
                    Integer line = currentLine < 0 ? null : currentLine;
                    rows.add(new Object[]{className, thisMethodSignature(), line == null ? "" : line, target});
                    String callerToken = name + "(" + parseDescriptorParams(descriptor) + ")";
                    resultList.add(new UsageResult(jarPath, internalName, line, "Method", callerToken));
                }
            };
        }
    }

    private static String parseDescriptorParams(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return "";
        }
        int start = descriptor.indexOf('(');
        int end = descriptor.indexOf(')');
        if (start < 0 || end <= start) {
            return "";
        }
        String desc = descriptor.substring(start + 1, end);
        if (desc.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < desc.length()) {
            int arrayDim = 0;
            while (i < desc.length() && desc.charAt(i) == '[') {
                arrayDim++;
                i++;
            }
            if (i >= desc.length()) {
                break;
            }
            char c = desc.charAt(i);
            String type;
            if (c == 'L') {
                int semi = desc.indexOf(';', i);
                if (semi < 0) {
                    break;
                }
                String name = desc.substring(i + 1, semi);
                int lastSlash = name.lastIndexOf('/');
                type = lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
                i = semi + 1;
            } else {
                type = switch (c) {
                    case 'I' -> "int";
                    case 'J' -> "long";
                    case 'Z' -> "boolean";
                    case 'B' -> "byte";
                    case 'C' -> "char";
                    case 'S' -> "short";
                    case 'F' -> "float";
                    case 'D' -> "double";
                    default -> "";
                };
                i++;
            }
            if (type.isEmpty()) {
                continue;
            }
            if (arrayDim > 0) {
                type += "[]".repeat(arrayDim);
            }
            if (result.length() > 0) {
                result.append(",");
            }
            result.append(type);
        }
        return result.toString();
    }
}
