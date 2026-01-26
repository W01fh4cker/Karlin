package com.karlin.decompiler.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FindReplaceDialog extends JDialog {
    private final RSyntaxTextArea textArea;
    private final JTextField findField = new JTextField(22);
    private final JTextField replaceField = new JTextField(22);
    private final JCheckBox regexBox = new JCheckBox("Regex");
    private final JCheckBox matchCaseBox = new JCheckBox("Match Case");
    private final JLabel statusLabel = new JLabel(" ");

    public FindReplaceDialog(JFrame owner, RSyntaxTextArea textArea) {
        super(owner, "Find / Replace", ModalityType.MODELESS);
        this.textArea = textArea;
        setSize(520, 220);
        setResizable(true);
        setLocationRelativeTo(owner);
        initLayout();
    }

    private void initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Find:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(findField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Replace:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(replaceField, gbc);

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options.add(regexBox);
        options.add(matchCaseBox);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(options, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton nextButton = new JButton("Next");
        JButton prevButton = new JButton("Prev");
        JButton replaceButton = new JButton("Replace");
        JButton replaceAllButton = new JButton("Replace All");
        JButton allButton = new JButton("Find All");
        JButton closeButton = new JButton("Close");
        buttons.add(nextButton);
        buttons.add(prevButton);
        buttons.add(replaceButton);
        buttons.add(replaceAllButton);
        buttons.add(allButton);
        buttons.add(closeButton);

        nextButton.addActionListener(event -> findNext(true));
        prevButton.addActionListener(event -> findNext(false));
        replaceButton.addActionListener(event -> replace(false));
        replaceAllButton.addActionListener(event -> replace(true));
        allButton.addActionListener(event -> findAll());
        closeButton.addActionListener(event -> setVisible(false));

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttons, BorderLayout.NORTH);
        south.add(statusLabel, BorderLayout.SOUTH);

        add(panel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    public void setReplaceVisible(boolean visible) {
        replaceField.setVisible(visible);
        revalidate();
        repaint();
    }

    private void findNext(boolean forward) {
        SearchContext context = createContext();
        context.setSearchForward(forward);
        context.setMarkAll(true);
        boolean found = SearchEngine.find(textArea, context).wasFound();
        updateStatus(found);
    }

    private void findAll() {
        SearchContext context = createContext();
        context.setMarkAll(true);
        SearchEngine.find(textArea, context);
        updateStatus(true);
    }

    private void replace(boolean all) {
        SearchContext context = createContext();
        context.setReplaceWith(replaceField.getText());
        context.setMarkAll(true);
        if (all) {
            SearchEngine.replaceAll(textArea, context);
        } else {
            SearchEngine.replace(textArea, context);
        }
        updateStatus(true);
    }

    private SearchContext createContext() {
        SearchContext context = new SearchContext();
        context.setSearchFor(findField.getText());
        context.setRegularExpression(regexBox.isSelected());
        context.setMatchCase(matchCaseBox.isSelected());
        context.setMarkAll(true);
        return context;
    }

    private void updateStatus(boolean found) {
        int count = countMatches();
        if (!found) {
            statusLabel.setText("Not found");
        } else {
            statusLabel.setText("Matches: " + count);
        }
    }

    private int countMatches() {
        String text = textArea.getText();
        String pattern = findField.getText();
        if (text == null || text.isEmpty() || pattern == null || pattern.isEmpty()) {
            return 0;
        }
        try {
            Pattern p = regexBox.isSelected()
                ? Pattern.compile(pattern, matchCaseBox.isSelected() ? 0 : Pattern.CASE_INSENSITIVE)
                : Pattern.compile(Pattern.quote(pattern), matchCaseBox.isSelected() ? 0 : Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            int count = 0;
            while (m.find()) {
                count++;
            }
            return count;
        } catch (Exception ignored) {
            return 0;
        }
    }
}
