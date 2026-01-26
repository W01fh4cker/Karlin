package com.karlin.decompiler.ui;

import com.karlin.decompiler.core.DecompileCache;
import com.karlin.decompiler.core.DecompileExecutor;
import com.karlin.decompiler.core.IndexModel;
import com.karlin.decompiler.core.JarClassReader;
import com.karlin.decompiler.engine.DecompilerEngine;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class SearchWindow extends JDialog {
    private final Supplier<IndexModel> indexSupplier;
    private final DecompileExecutor executor;
    private final DecompileCache cache;
    private final JComboBox<DecompilerEngine> engineCombo;
    private final JComboBox<Charset> charsetCombo;

    private final JTextField queryField = new JTextField(40);
    private final JCheckBox regexBox = new JCheckBox("Regex");
    private final JTextField prefixField = new JTextField(30);
    private final JCheckBox classBox = new JCheckBox("Class", true);
    private final JCheckBox methodBox = new JCheckBox("Method", true);
    private final JCheckBox fieldBox = new JCheckBox("Field", true);
    private final JCheckBox codeBox = new JCheckBox("Code", true);
    private final JCheckBox resourceBox = new JCheckBox("Resource", true);
    private final JButton searchButton = new JButton("Search");
    private final JLabel statusLabel = new JLabel(" ");

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Type", "Class", "Member/Path", "Detail"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(tableModel);
    private final javax.swing.JProgressBar progressBar = new javax.swing.JProgressBar();
    private final List<SearchResult> results = new ArrayList<>();
    private final Consumer<SearchResult> onOpen;

    public SearchWindow(JFrame owner,
                        Supplier<IndexModel> indexSupplier,
                        DecompileExecutor executor,
                        DecompileCache cache,
                        JComboBox<DecompilerEngine> engineCombo,
                        JComboBox<Charset> charsetCombo,
                        Consumer<SearchResult> onOpen) {
        super(owner, "Search", ModalityType.MODELESS);
        this.indexSupplier = indexSupplier;
        this.executor = executor;
        this.cache = cache;
        this.engineCombo = engineCombo;
        this.charsetCombo = charsetCombo;
        this.onOpen = onOpen;

        setSize(900, 600);
        setMinimumSize(new java.awt.Dimension(700, 400));
        setResizable(true);
        setLocationRelativeTo(owner);
        initLayout();
        setResizable(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    private void initLayout() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        topPanel.add(new JLabel("Query:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(queryField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(regexBox, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        topPanel.add(new JLabel("Prefixes:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(prefixField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(new JLabel("comma-separated"), gbc);
        row++;

        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scopePanel.add(classBox);
        scopePanel.add(methodBox);
        scopePanel.add(fieldBox);
        scopePanel.add(codeBox);
        scopePanel.add(resourceBox);

        gbc.gridx = 0;
        gbc.gridy = row;
        topPanel.add(new JLabel("Scope:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(scopePanel, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(searchButton, gbc);

        searchButton.addActionListener(event -> runSearch());
        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = resultTable.getSelectedRow();
                    if (row >= 0 && row < results.size() && onOpen != null) {
                        onOpen.accept(results.get(row));
                    }
                }
            }
        });

        progressBar.setStringPainted(true);
        progressBar.setString("0/0");

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(progressBar, BorderLayout.NORTH);
        south.add(statusLabel, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    private void runSearch() {
        IndexModel index = indexSupplier.get();
        if (index == null) {
            statusLabel.setText("Scan a directory first");
            return;
        }
        String query = queryField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Enter a query");
            return;
        }

        Pattern pattern = null;
        if (regexBox.isSelected()) {
            try {
                pattern = Pattern.compile(query);
            } catch (Exception e) {
                statusLabel.setText("Invalid regex: " + e.getMessage());
                return;
            }
        }

        List<String> prefixes = parsePrefixes(prefixField.getText());
        boolean searchClass = classBox.isSelected();
        boolean searchMethod = methodBox.isSelected();
        boolean searchField = fieldBox.isSelected();
        boolean searchCode = codeBox.isSelected();
        boolean searchResource = resourceBox.isSelected();

        tableModel.setRowCount(0);
        results.clear();
        statusLabel.setText("Searching...");

        Pattern finalPattern = pattern;
        executor.submit(() -> {
            List<Object[]> rows = new ArrayList<>();
            List<SearchResult> resultList = new ArrayList<>();
            Set<String> dedupe = new HashSet<>();
            DecompilerEngine engine = (DecompilerEngine) engineCombo.getSelectedItem();
            Charset charset = (Charset) charsetCombo.getSelectedItem();
            if (charset == null) {
                charset = StandardCharsets.UTF_8;
            }

            int totalClasses = index.getJars().stream().mapToInt(jar -> jar.getClassInternalNames().size()).sum();
            int progressMax = Math.max(1, totalClasses);
            java.util.concurrent.atomic.AtomicInteger processed = new java.util.concurrent.atomic.AtomicInteger();
            SwingUtilities.invokeLater(() -> {
                progressBar.setMinimum(0);
                progressBar.setMaximum(progressMax);
                progressBar.setValue(0);
                progressBar.setString("0/" + totalClasses);
            });

            for (var jar : index.getJars()) {
                for (String internalName : jar.getClassInternalNames()) {
                    String className = internalName.replace('/', '.');
                    if (!matchesPrefix(internalName, className, prefixes)) {
                        continue;
                    }
                    try {
                        byte[] bytes = JarClassReader.readClassBytes(jar.getJarPath(), internalName);
                        ClassReader reader = new ClassReader(bytes);
                        reader.accept(new SearchClassVisitor(rows, resultList, dedupe, jar.getJarPath(), className,
                            internalName, query, finalPattern, searchClass, searchMethod, searchField),
                            ClassReader.SKIP_FRAMES);
                    } catch (Exception ignored) {
                    }

                    if (searchCode && engine != null) {
                        String source = cache.get(engine.getId(), jar.getJarPath(), internalName);
                        if (source == null) {
                            try {
                                source = engine.decompile(jar.getJarPath(), internalName, StandardCharsets.UTF_8);
                                cache.put(engine.getId(), jar.getJarPath(), internalName, source);
                            } catch (Exception e) {
                                source = "";
                            }
                        }
                        addCodeMatches(rows, resultList, dedupe, jar.getJarPath(), className, internalName,
                            source, query, finalPattern);
                    }
                    int done = processed.incrementAndGet();
                    if (done % 50 == 0) {
                        int value = done;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(value);
                            progressBar.setString(value + "/" + totalClasses);
                        });
                    }
                }

                if (searchResource) {
                    searchResources(rows, resultList, dedupe, jar.getJarPath(), query, finalPattern, prefixes, charset);
                }
            }
            List<Object[]> safeRows = rows;
            List<SearchResult> safeResults = resultList;
            SwingUtilities.invokeLater(() -> {
                for (Object[] row : safeRows) {
                    tableModel.addRow(row);
                }
                results.addAll(safeResults);
                progressBar.setValue(progressMax);
                progressBar.setString(totalClasses + "/" + totalClasses);
                statusLabel.setText("Results: " + safeResults.size());
            });
            return null;
        });
    }

    private void addCodeMatches(List<Object[]> rows,
                                List<SearchResult> resultList,
                                Set<String> dedupe,
                                Path jarPath,
                                String className,
                                String internalName,
                                String source,
                                String query,
                                Pattern pattern) {
        if (source == null || source.isEmpty()) {
            return;
        }
        String[] lines = source.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (matches(line, query, pattern)) {
                String detail = "Line " + (i + 1) + ": " + line.trim();
                addResult(rows, resultList, dedupe,
                    new SearchResult("Code", jarPath, internalName, "", detail, i + 1),
                    className, "", detail);
            }
        }
    }

    private void searchResources(List<Object[]> rows,
                                 List<SearchResult> resultList,
                                 Set<String> dedupe,
                                 Path jarPath,
                                 String query,
                                 Pattern pattern,
                                 List<String> prefixes,
                                 Charset charset) {
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().endsWith(".class")) {
                    continue;
                }
                if (!matchesPrefix(entry.getName(), entry.getName(), prefixes)) {
                    continue;
                }
                byte[] bytes = zip.getInputStream(entry).readAllBytes();
                String text;
                try {
                    text = new String(bytes, charset);
                } catch (Exception e) {
                    continue;
                }
                if (matches(text, query, pattern)) {
                    addResult(rows, resultList, dedupe,
                        new SearchResult("Resource", jarPath, null, entry.getName(), "", null),
                        jarPath.getFileName().toString(), entry.getName(), "");
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void addResult(List<Object[]> rows,
                                  List<SearchResult> resultList,
                                  Set<String> dedupe,
                                  SearchResult result,
                                  String className,
                                  String memberOrPath,
                                  String detail) {
        String jarKey = result.getJarPath() == null ? "" : result.getJarPath().toString();
        String key = result.getType() + "|" + jarKey + "|" + className + "|" + memberOrPath + "|" + detail;
        if (!dedupe.add(key)) {
            return;
        }
        rows.add(new Object[]{result.getType(), className, memberOrPath, detail});
        resultList.add(result);
    }

    private boolean matches(String text, String query, Pattern pattern) {
        if (pattern != null) {
            return pattern.matcher(text).find();
        }
        return text.contains(query);
    }

    private List<String> parsePrefixes(String input) {
        List<String> prefixes = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return prefixes;
        }
        String[] parts = input.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                prefixes.add(trimmed);
            }
        }
        return prefixes;
    }

    private boolean matchesPrefix(String internalName, String dotName, List<String> prefixes) {
        if (prefixes.isEmpty()) {
            return true;
        }
        for (String prefix : prefixes) {
            String normalized = prefix.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            String slashPrefix = normalized.replace('.', '/');
            if (internalName.startsWith(slashPrefix)) {
                return true;
            }
            if (dotName.startsWith(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static final class SearchClassVisitor extends ClassVisitor {
        private final List<Object[]> rows;
        private final List<SearchResult> resultList;
        private final Set<String> dedupe;
        private final Path jarPath;
        private final String className;
        private final String internalName;
        private final String query;
        private final Pattern pattern;
        private final boolean searchClass;
        private final boolean searchMethod;
        private final boolean searchField;

        private SearchClassVisitor(List<Object[]> rows,
                                   List<SearchResult> resultList,
                                   Set<String> dedupe,
                                   Path jarPath,
                                   String className,
                                   String internalName,
                                   String query,
                                   Pattern pattern,
                                   boolean searchClass,
                                   boolean searchMethod,
                                   boolean searchField) {
            super(Opcodes.ASM9);
            this.rows = rows;
            this.resultList = resultList;
            this.dedupe = dedupe;
            this.jarPath = jarPath;
            this.className = className;
            this.internalName = internalName;
            this.query = query;
            this.pattern = pattern;
            this.searchClass = searchClass;
            this.searchMethod = searchMethod;
            this.searchField = searchField;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (searchClass && matches(className, query, pattern)) {
                addResult(rows, resultList, dedupe,
                    new SearchResult("Class", jarPath, internalName, "", "", null),
                    className, "", "");
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (searchField && matches(name, query, pattern)) {
                addResult(rows, resultList, dedupe,
                    new SearchResult("Field", jarPath, internalName, name, descriptor, null),
                    className, name, descriptor);
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (searchMethod && matches(name, query, pattern)) {
                addResult(rows, resultList, dedupe,
                    new SearchResult("Method", jarPath, internalName, name, descriptor, null),
                    className, name, descriptor);
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        private boolean matches(String text, String query, Pattern pattern) {
            if (pattern != null) {
                return pattern.matcher(text).find();
            }
            return text.contains(query);
        }
    }
}
