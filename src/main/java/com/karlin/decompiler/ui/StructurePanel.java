package com.karlin.decompiler.ui;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class StructurePanel extends JPanel {
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Type", "Name", "Descriptor"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final List<StructureEntry> entries = new ArrayList<>();
    private Consumer<StructureEntry> onSelect;

    public StructurePanel() {
        super(new BorderLayout());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < entries.size() && onSelect != null) {
                onSelect.accept(entries.get(row));
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setOnSelect(Consumer<StructureEntry> onSelect) {
        this.onSelect = onSelect;
    }

    public void setEntries(List<StructureEntry> newEntries) {
        SwingUtilities.invokeLater(() -> {
            entries.clear();
            tableModel.setRowCount(0);
            entries.addAll(newEntries);
            for (StructureEntry entry : newEntries) {
                tableModel.addRow(new Object[]{entry.getKind(), entry.getName(), entry.getDescriptor()});
            }
        });
    }
}
