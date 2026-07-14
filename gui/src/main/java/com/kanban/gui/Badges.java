package com.kanban.gui;

import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

/** Small colored pill labels used to show a task's category or status. */
final class Badges {

    private Badges() {
    }

    static JLabel category(TaskCategory category) {
        JLabel badge = new JLabel(category.label());
        badge.setOpaque(true);
        badge.setBackground(Theme.categoryBackground(category));
        badge.setForeground(Theme.categoryForeground(category));
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 10f));
        badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        return badge;
    }

    static JLabel status(TaskStatus status) {
        JLabel badge = new JLabel(status.label());
        badge.setOpaque(true);
        badge.setBackground(Theme.statusColor(status));
        badge.setForeground(Theme.TEXT_PRIMARY);
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 10f));
        badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        return badge;
    }
}
