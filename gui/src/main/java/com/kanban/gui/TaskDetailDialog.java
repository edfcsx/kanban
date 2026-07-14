package com.kanban.gui;

import com.kanban.core.Task;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Read-only "attributes" view for a single task, opened by clicking its
 * card. Description is rendered as markdown so embedded code snippets show
 * up formatted rather than as raw ``` fences.
 */
final class TaskDetailDialog {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private TaskDetailDialog() {
    }

    static void show(Component parent, Task task, Consumer<Task> onEdit) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), task.getTitle(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(0, 12));
        dialog.getContentPane().setBackground(Theme.SURFACE);

        JPanel content = new JPanel();
        content.setBackground(Theme.SURFACE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 16, 8, 16));

        JLabel titleLabel = new JLabel(task.getTitle());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badgeRow.setOpaque(false);
        badgeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        badgeRow.add(Badges.status(task.getStatus()));
        badgeRow.add(Badges.category(task.getCategory()));

        JLabel metaLabel = new JLabel("Created " + DATE_FORMAT.format(task.getCreatedAt())
                + "   ·   Updated " + DATE_FORMAT.format(task.getUpdatedAt()));
        metaLabel.setFont(metaLabel.getFont().deriveFont(10f));
        metaLabel.setForeground(Theme.TEXT_MUTED);
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JEditorPane descriptionPane = new JEditorPane("text/html", MarkdownRenderer.toHtml(task.getDescription()));
        descriptionPane.setEditable(false);
        descriptionPane.setBorder(new EmptyBorder(4, 2, 4, 2));
        descriptionPane.setCaretPosition(0);

        JScrollPane descriptionScroll = new JScrollPane(descriptionPane);
        descriptionScroll.setPreferredSize(new Dimension(480, 320));
        descriptionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionScroll.setBorder(javax.swing.BorderFactory.createLineBorder(Theme.BORDER));

        content.add(titleLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(badgeRow);
        content.add(Box.createVerticalStrut(4));
        content.add(metaLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(descriptionScroll);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonBar.setBackground(Theme.SURFACE);
        buttonBar.setBorder(new EmptyBorder(0, 16, 12, 16));

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> {
            dialog.dispose();
            onEdit.accept(task);
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        buttonBar.add(editButton);
        buttonBar.add(closeButton);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(buttonBar, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(closeButton);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
