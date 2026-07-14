package com.kanban.gui;

import com.kanban.core.KanbanRepository;
import com.kanban.core.Task;
import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The whole board: a toolbar with an "add task" action, and three columns
 * (Todo / In Progress / Completed). Since the CLI can add or change tasks
 * from another process at any time, the board polls the database on a timer
 * and only re-renders when the on-disk state actually changed.
 */
public final class MainFrame extends JFrame {

    private static final int REFRESH_INTERVAL_MS = 2000;

    private final KanbanRepository repository;
    private final Map<TaskStatus, JPanel> columnBodies = new EnumMap<>(TaskStatus.class);
    private final Map<TaskStatus, JLabel> columnHeaders = new EnumMap<>(TaskStatus.class);
    private List<Task> tasks;
    private String searchQuery = "";

    public MainFrame(KanbanRepository repository) {
        super("Kanban");
        this.repository = repository;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 680);
        setMinimumSize(new Dimension(720, 480));
        setLocationRelativeTo(null);

        getContentPane().setBackground(Theme.WINDOW_BACKGROUND);
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildBoard(), BorderLayout.CENTER);

        this.tasks = repository.loadAll();
        renderTasks();

        Timer refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> reloadFromDisk());
        refreshTimer.start();
    }

    private JComponent buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(Theme.WINDOW_BACKGROUND);
        toolbar.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Kanban Board");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pathLabel = new JLabel(repository.getDatabaseFile().getAbsolutePath());
        pathLabel.setFont(pathLabel.getFont().deriveFont(10f));
        pathLabel.setForeground(Theme.TEXT_MUTED);
        pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(pathLabel);

        JTextField searchField = new JTextField();
        searchField.setToolTipText("Search tasks by title or description");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onSearchChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onSearchChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onSearchChanged();
            }

            private void onSearchChanged() {
                searchQuery = searchField.getText().trim();
                renderTasks();
            }
        });

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(new EmptyBorder(0, 24, 0, 24));
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setForeground(Theme.TEXT_SECONDARY);
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JButton addButton = new JButton("+ New Task");
        addButton.setForeground(Theme.TEXT_PRIMARY);
        addButton.addActionListener(e -> openAddDialog());

        toolbar.add(titleBlock, BorderLayout.WEST);
        toolbar.add(searchPanel, BorderLayout.CENTER);
        toolbar.add(addButton, BorderLayout.EAST);
        return toolbar;
    }

    private JComponent buildBoard() {
        JPanel board = new JPanel(new GridLayout(1, 3, 12, 0));
        board.setBackground(Theme.WINDOW_BACKGROUND);
        board.setBorder(new EmptyBorder(0, 12, 12, 12));
        for (TaskStatus status : TaskStatus.values()) {
            board.add(buildColumn(status));
        }
        return board;
    }

    private JComponent buildColumn(TaskStatus status) {
        JPanel column = new JPanel(new BorderLayout());
        column.setBackground(Theme.SURFACE);
        column.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JLabel header = new JLabel(status.label());
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setForeground(Theme.TEXT_PRIMARY);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));
        header.setOpaque(true);
        header.setBackground(headerColor(status));
        columnHeaders.put(status, header);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.SURFACE);
        body.setBorder(new EmptyBorder(8, 8, 8, 8));
        columnBodies.put(status, body);

        JPanel bodyWrapper = new JPanel(new BorderLayout());
        bodyWrapper.setBackground(Theme.SURFACE);
        bodyWrapper.add(body, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(bodyWrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        column.add(header, BorderLayout.NORTH);
        column.add(scrollPane, BorderLayout.CENTER);
        return column;
    }

    private Color headerColor(TaskStatus status) {
        return Theme.statusColor(status);
    }

    private void renderTasks() {
        for (TaskStatus status : TaskStatus.values()) {
            JPanel body = columnBodies.get(status);
            body.removeAll();

            List<Task> columnTasks = tasks.stream()
                    .filter(t -> t.getStatus() == status)
                    .filter(this::matchesSearch)
                    .sorted(Comparator.comparing(Task::getCreatedAt))
                    .toList();

            for (Task task : columnTasks) {
                TaskCardPanel card = new TaskCardPanel(task, this::openDetailDialog,
                        this::moveToPrevious, this::moveToNext, this::openEditDialog, this::confirmAndDelete);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(card);
                body.add(Box.createVerticalStrut(6));
            }

            columnHeaders.get(status).setText(status.label() + "  (" + columnTasks.size() + ")");
            body.revalidate();
            body.repaint();
        }
    }

    private boolean matchesSearch(Task task) {
        if (searchQuery.isEmpty()) {
            return true;
        }
        String query = searchQuery.toLowerCase(Locale.ROOT);
        return task.getTitle().toLowerCase(Locale.ROOT).contains(query)
                || task.getDescription().toLowerCase(Locale.ROOT).contains(query);
    }

    private void reloadFromDisk() {
        List<Task> fresh = repository.loadAll();
        if (!fresh.equals(tasks)) {
            tasks = fresh;
            renderTasks();
        }
    }

    private void openAddDialog() {
        TaskDialog.Result result = TaskDialog.show(this, "New Task", "", "", TaskCategory.NONE);
        if (result == null) {
            return;
        }
        repository.addTask(result.title(), result.description(), result.category());
        refreshAfterOwnAction();
    }

    private void openEditDialog(Task task) {
        TaskDialog.Result result = TaskDialog.show(this, "Edit Task", task.getTitle(), task.getDescription(),
                task.getCategory());
        if (result == null) {
            return;
        }
        repository.updateTask(task.getId(), result.title(), result.description(), result.category());
        refreshAfterOwnAction();
    }

    private void openDetailDialog(Task task) {
        TaskDetailDialog.show(this, task, this::openEditDialog);
    }

    private void moveToPrevious(Task task) {
        repository.updateStatus(task.getId(), task.getStatus().previous());
        refreshAfterOwnAction();
    }

    private void moveToNext(Task task) {
        repository.updateStatus(task.getId(), task.getStatus().next());
        refreshAfterOwnAction();
    }

    private void confirmAndDelete(Task task) {
        int option = JOptionPane.showConfirmDialog(this,
                "Delete \"" + task.getTitle() + "\"?", "Confirm delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option == JOptionPane.YES_OPTION) {
            repository.deleteTask(task.getId());
            refreshAfterOwnAction();
        }
    }

    private void refreshAfterOwnAction() {
        tasks = repository.loadAll();
        renderTasks();
    }
}