package com.karlin.decompiler.ui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;

public final class ClassKindIcon implements Icon {
    private final Color color;
    private final String text;

    public ClassKindIcon(Color color, String text) {
        this.color = color;
        this.text = text;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        int size = getIconWidth();
        g.setColor(color);
        g.fillOval(x, y, size, size);
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (size - fm.stringWidth(text)) / 2;
        int ty = y + (size + fm.getAscent() - fm.getDescent()) / 2 - 1;
        g.drawString(text, tx, ty);
    }

    @Override
    public int getIconWidth() {
        return 14;
    }

    @Override
    public int getIconHeight() {
        return 14;
    }
}
