package com.kanban.gui;

import com.kanban.core.KanbanRepository;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

public final class KanbanApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Deliberately NOT the system L&F: on a dark OS theme its
                // default text color turns light, which is unreadable
                // against this app's fixed light card backgrounds. Metal
                // renders identically (light) on every OS/theme.
                UIManager.setLookAndFeel(new MetalLookAndFeel());
            } catch (Exception ignored) {
                // Keep the JVM's default look and feel.
            }
            MainFrame frame = new MainFrame(new KanbanRepository());
            frame.setVisible(true);
        });
    }
}