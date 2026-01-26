package com.karlin.decompiler;

import com.formdev.flatlaf.FlatLightLaf;
import com.karlin.decompiler.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("TitlePane.unifiedBackground", true);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
