package com.kanban.gui;

import com.kanban.core.Task;
import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * A single visual card for one task, shown inside a column. Movement between
 * columns is done with the prev/next buttons rather than drag-and-drop, to
 * keep the interaction simple and discoverable. Clicking anywhere on the
 * card (outside the action buttons) opens the read-only detail view.
 */
final class TaskCardPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());
    private static final int DESCRIPTION_PREVIEW_LIMIT = 200;

    TaskCardPanel(Task task, Consumer<Task> onView, Consumer<Task> onMovePrevious, Consumer<Task> onMoveNext,
                  Consumer<Task> onEdit, Consumer<Task> onDelete) {
        setLayout(new BorderLayout(6, 6));
        setBorder(new CompoundBorder(new EmptyBorder(4, 4, 4, 4),
                new CompoundBorder(new LineBorder(Theme.BORDER, 1, true), new EmptyBorder(8, 10, 8, 10))));
        setBackground(Theme.SURFACE);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onView.accept(task);
            }
        });

        add(buildTextBlock(task), BorderLayout.CENTER);
        add(buildActionBar(task, onMovePrevious, onMoveNext, onEdit, onDelete), BorderLayout.SOUTH);
    }

    private JPanel buildTextBlock(Task task) {
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        if (task.getCategory() != TaskCategory.NONE) {
            JLabel categoryBadge = Badges.category(task.getCategory());
            categoryBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(categoryBadge);
            textPanel.add(Box.createVerticalStrut(4));
        }

        JLabel titleLabel = new JLabel("<html><b>" + escape(task.getTitle()) + "</b></html>");
        titleLabel.setFont(titleLabel.getFont().deriveFont(14f));
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLabel);

        String description = task.getDescription();
        if (description != null && !description.isBlank()) {
            JTextArea descArea = new JTextArea(truncate(description, DESCRIPTION_PREVIEW_LIMIT));
            descArea.setEditable(false);
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);
            descArea.setOpaque(false);
            descArea.setFont(descArea.getFont().deriveFont(12f));
            descArea.setForeground(Theme.TEXT_SECONDARY);
            descArea.setBorder(null);
            descArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(Box.createVerticalStrut(4));
            textPanel.add(descArea);
        }

        JLabel dateLabel = new JLabel(DATE_FORMAT.format(task.getUpdatedAt()));
        dateLabel.setFont(dateLabel.getFont().deriveFont(10f));
        dateLabel.setForeground(Theme.TEXT_MUTED);
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(dateLabel);

        return textPanel;
    }

    private JPanel buildActionBar(Task task, Consumer<Task> onMovePrevious, Consumer<Task> onMoveNext,
                                  Consumer<Task> onEdit, Consumer<Task> onDelete) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        bar.setOpaque(false);

        JButton previous = smallButton("‹ Move");
        previous.setToolTipText("Move to previous column");
        previous.setEnabled(task.getStatus() != TaskStatus.TODO);
        previous.addActionListener(e -> onMovePrevious.accept(task));

        JButton next = smallButton("Move ›");
        next.setToolTipText("Move to next column");
        next.setEnabled(task.getStatus() != TaskStatus.COMPLETED);
        next.addActionListener(e -> onMoveNext.accept(task));

        JButton edit = smallButton("Edit");
        edit.addActionListener(e -> onEdit.accept(task));

        JButton delete = smallButton("Delete");
        delete.addActionListener(e -> onDelete.accept(task));

        bar.add(previous);
        bar.add(next);
        bar.add(edit);
        bar.add(delete);
        return bar;
    }

    private JButton smallButton(String text) {
        JButton button = new JButton(text);
        button.setMargin(new Insets(2, 6, 2, 6));
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(11f));
        button.setForeground(Theme.TEXT_PRIMARY);
        return button;
    }

    private static String truncate(String text, int limit) {
        if (text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit).stripTrailing() + "…";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}