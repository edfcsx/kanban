package com.kanban.gui;

import com.kanban.core.KanbanRepository;
import com.kanban.core.Task;
import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The whole board: a toolbar with an "add task" action, and three columns
 * (Todo / In Progress / Completed). Since the CLI can add or change tasks
 * from another process at any time, the board polls the database on a timer
 * and only re-renders when the on-disk state actually changed.
 */
final class MainView extends BorderPane {

    private static final Duration REFRESH_INTERVAL = Duration.seconds(2);

    private final KanbanRepository repository;
    private final Map<TaskStatus, VBox> columnBodies = new EnumMap<>(TaskStatus.class);
    private final Map<TaskStatus, Label> columnHeaders = new EnumMap<>(TaskStatus.class);
    private final Map<TaskStatus, VBox> columns = new EnumMap<>(TaskStatus.class);
    private List<Task> tasks;
    private String searchQuery = "";
    private TaskCategory categoryFilter;

    MainView(KanbanRepository repository) {
        this.repository = repository;

        setTop(buildToolbar());
        setCenter(buildBoard());

        this.tasks = repository.loadAll();
        renderTasks();

        Timeline refreshTimeline = new Timeline(new KeyFrame(REFRESH_INTERVAL, e -> reloadFromDisk()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(16);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Kanban Board");
        title.getStyleClass().add("app-title");

        Label pathLabel = new Label(repository.getDatabaseFile().getAbsolutePath());
        pathLabel.getStyleClass().add("app-subtitle");

        VBox titleBlock = new VBox(2, title, pathLabel);

        Label searchLabel = new Label("Search");
        searchLabel.getStyleClass().add("search-label");

        TextField searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("Search tasks by title or description");
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchQuery = newVal.trim();
            renderTasks();
        });

        HBox searchBox = new HBox(6, searchLabel, searchField);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        Label categoryLabel = new Label("Category");
        categoryLabel.getStyleClass().add("search-label");

        ComboBox<TaskCategory> categoryCombo = new ComboBox<>(
                FXCollections.observableArrayList(TaskCategory.values()));
        categoryCombo.getStyleClass().add("form-combo");
        categoryCombo.getItems().add(0, null);
        categoryCombo.setValue(null);
        categoryCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(TaskCategory category) {
                return category == null ? "All Categories" : category.label();
            }

            @Override
            public TaskCategory fromString(String string) {
                return null;
            }
        });
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            categoryFilter = newVal;
            renderTasks();
        });

        HBox categoryBox = new HBox(6, categoryLabel, categoryCombo);
        categoryBox.setAlignment(Pos.CENTER_LEFT);

        Button addButton = new Button("+ New Task");
        addButton.getStyleClass().add("btn-primary");
        addButton.setOnAction(e -> openAddDialog());

        toolbar.getChildren().addAll(titleBlock, searchBox, categoryBox, addButton);
        return toolbar;
    }

    private HBox buildBoard() {
        HBox board = new HBox(12);
        board.getStyleClass().add("board");
        for (TaskStatus status : TaskStatus.values()) {
            VBox column = buildColumn(status);
            HBox.setHgrow(column, Priority.ALWAYS);
            board.getChildren().add(column);
        }
        return board;
    }

    private VBox buildColumn(TaskStatus status) {
        Label header = new Label(status.label());
        header.getStyleClass().addAll("column-header", Theme.columnHeaderStyleClass(status));
        header.setMaxWidth(Double.MAX_VALUE);
        columnHeaders.put(status, header);

        VBox body = new VBox(8);
        body.getStyleClass().add("column-body");
        body.setPadding(new Insets(10));
        columnBodies.put(status, body);

        ScrollPane scrollPane = new ScrollPane(body);
        scrollPane.getStyleClass().add("column-scroll");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox column = new VBox(header, scrollPane);
        column.getStyleClass().add("column");
        columns.put(status, column);

        scrollPane.setOnDragOver(e -> {
            if (e.getDragboard().hasContent(TaskCard.TASK_ID_FORMAT)) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        scrollPane.setOnDragEntered(e -> {
            if (e.getDragboard().hasContent(TaskCard.TASK_ID_FORMAT)) {
                column.getStyleClass().add("column-drag-over");
            }
        });
        scrollPane.setOnDragExited(e -> column.getStyleClass().remove("column-drag-over"));
        scrollPane.setOnDragDropped(e -> {
            var dragboard = e.getDragboard();
            boolean success = dragboard.hasContent(TaskCard.TASK_ID_FORMAT);
            if (success) {
                moveTaskToStatus((String) dragboard.getContent(TaskCard.TASK_ID_FORMAT), status);
            }
            e.setDropCompleted(success);
            e.consume();
        });

        return column;
    }

    private void renderTasks() {
        for (TaskStatus status : TaskStatus.values()) {
            VBox body = columnBodies.get(status);
            body.getChildren().clear();

            List<Task> columnTasks = tasks.stream()
                    .filter(t -> t.getStatus() == status)
                    .filter(this::matchesSearch)
                    .filter(this::matchesCategory)
                    .sorted(Comparator.comparing(Task::getCreatedAt))
                    .toList();

            for (Task task : columnTasks) {
                TaskCard card = new TaskCard(task, this::openDetailDialog,
                        this::moveToPrevious, this::moveToNext, this::openEditDialog, this::confirmAndDelete);
                body.getChildren().add(card);
            }

            columnHeaders.get(status).setText(status.label() + "  (" + columnTasks.size() + ")");
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

    private boolean matchesCategory(Task task) {
        return categoryFilter == null || task.getCategory() == categoryFilter;
    }

    private void reloadFromDisk() {
        List<Task> fresh = repository.loadAll();
        if (!fresh.equals(tasks)) {
            tasks = fresh;
            renderTasks();
        }
    }

    private Window owner() {
        return getScene() != null ? getScene().getWindow() : null;
    }

    private void openAddDialog() {
        TaskDialog.Result result = TaskDialog.show(owner(), "New Task", "", "", TaskCategory.NONE);
        if (result == null) {
            return;
        }
        repository.addTask(result.title(), result.description(), result.category());
        refreshAfterOwnAction();
    }

    private void openEditDialog(Task task) {
        TaskDialog.Result result = TaskDialog.show(owner(), "Edit Task", task.getTitle(), task.getDescription(),
                task.getCategory());
        if (result == null) {
            return;
        }
        repository.updateTask(task.getId(), result.title(), result.description(), result.category());
        refreshAfterOwnAction();
    }

    private void openDetailDialog(Task task) {
        TaskDetailView.show(owner(), task, this::openEditDialog);
    }

    private void moveToPrevious(Task task) {
        repository.updateStatus(task.getId(), task.getStatus().previous());
        refreshAfterOwnAction();
    }

    private void moveToNext(Task task) {
        repository.updateStatus(task.getId(), task.getStatus().next());
        refreshAfterOwnAction();
    }

    private void moveTaskToStatus(String taskId, TaskStatus newStatus) {
        boolean alreadyThere = tasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .anyMatch(t -> t.getStatus() == newStatus);
        if (alreadyThere) {
            return;
        }
        repository.updateStatus(taskId, newStatus);
        refreshAfterOwnAction();
    }

    private void confirmAndDelete(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + task.getTitle() + "\"?",
                ButtonType.YES, ButtonType.NO);
        alert.initOwner(owner());
        alert.setTitle("Confirm delete");
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(Styles.SHEET);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isPresent() && choice.get() == ButtonType.YES) {
            repository.deleteTask(task.getId());
            refreshAfterOwnAction();
        }
    }

    private void refreshAfterOwnAction() {
        tasks = repository.loadAll();
        renderTasks();
    }
}
