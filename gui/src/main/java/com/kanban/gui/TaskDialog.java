package com.kanban.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;

/** Modal form used for both creating and editing a task. */
final class TaskDialog {

    record Result(String title, String description) {
    }

    private TaskDialog() {
    }

    static Result show(Component parent, String dialogTitle, String initialTitle, String initialDescription) {
        JTextField titleField = new JTextField(initialTitle == null ? "" : initialTitle, 30);
        JTextArea descriptionArea = new JTextArea(initialDescription == null ? "" : initialDescription, 6, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JLabel("Title"));
        form.add(titleField);
        form.add(Box.createVerticalStrut(8));
        form.add(new JLabel("Description"));
        form.add(new JScrollPane(descriptionArea));

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
            return new Result(titleField.getText().trim(), descriptionArea.getText());
        }
    }
}