package com.kanban.gui;

import com.kanban.core.TaskCategory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** Modal form used for both creating and editing a task. */
final class TaskDialog {

    record Result(String title, String description, TaskCategory category) {
    }

    private TaskDialog() {
    }

    static Result show(Component parent, String dialogTitle, String initialTitle, String initialDescription,
                        TaskCategory initialCategory) {
        JTextField titleField = new JTextField(initialTitle == null ? "" : initialTitle, 30);

        JComboBox<TaskCategory> categoryCombo = new JComboBox<>(TaskCategory.values());
        categoryCombo.setSelectedItem(initialCategory == null ? TaskCategory.NONE : initialCategory);

        JTextArea descriptionArea = new JTextArea(initialDescription == null ? "" : initialDescription, 10, 34);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JEditorPane previewPane = new JEditorPane("text/html", "");
        previewPane.setEditable(false);
        previewPane.setBorder(new javax.swing.border.EmptyBorder(4, 6, 4, 6));

        JTabbedPane descriptionTabs = new JTabbedPane();
        descriptionTabs.addTab("Write", new JScrollPane(descriptionArea));
        descriptionTabs.addTab("Preview", new JScrollPane(previewPane));
        descriptionTabs.addChangeListener(e -> {
            if (descriptionTabs.getSelectedIndex() == 1) {
                previewPane.setText(MarkdownRenderer.toHtml(descriptionArea.getText()));
                previewPane.setCaretPosition(0);
            }
        });
        descriptionTabs.setPreferredSize(new Dimension(420, 220));

        JPanel categoryRow = new JPanel(new BorderLayout(8, 0));
        categoryRow.add(new JLabel("Category"), BorderLayout.WEST);
        categoryRow.add(categoryCombo, BorderLayout.CENTER);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JLabel("Title"));
        form.add(titleField);
        form.add(Box.createVerticalStrut(8));
        form.add(categoryRow);
        form.add(Box.createVerticalStrut(8));
        JLabel descLabel = new JLabel("Description (Markdown supported, e.g. ```code``` for snippets)");
        descLabel.setFont(descLabel.getFont().deriveFont(11f));
        form.add(descLabel);
        form.add(descriptionTabs);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form, BorderLayout.CENTER);

        while (true) {
            int option = JOptionPane.showConfirmDialog(parent, panel, dialogTitle,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return null;
            }
            if (titleField.getText().isBlank()) {
                JOptionPane.showMessageDialog(parent, "Title is required.", "Invalid task",
                        JOptionPane.WARNING_MESSAGE);
                continue;
            }
            return new Result(titleField.getText().trim(), descriptionArea.getText(),
                    (TaskCategory) categoryCombo.getSelectedItem());
        }
    }
}
