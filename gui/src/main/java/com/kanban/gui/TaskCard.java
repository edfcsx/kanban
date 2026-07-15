package com.kanban.gui;

import com.kanban.core.Task;
import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * A single visual card for one task, shown inside a column. Movement between
 * columns is done with the prev/next buttons rather than drag-and-drop, to
 * keep the interaction simple and discoverable. Clicking anywhere on the
 * card (outside the action buttons) opens the read-only detail view.
 */
final class TaskCard extends VBox {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());
    private static final int DESCRIPTION_PREVIEW_LIMIT = 200;

    TaskCard(Task task, Consumer<Task> onView, Consumer<Task> onMovePrevious, Consumer<Task> onMoveNext,
             Consumer<Task> onEdit, Consumer<Task> onDelete) {
        getStyleClass().add("task-card");
        setSpacing(4);
        setOnMouseClicked(e -> onView.accept(task));

        if (task.getCategory() != TaskCategory.NONE) {
            getChildren().add(Badges.category(task.getCategory()));
        }

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("task-title");
        titleLabel.setWrapText(true);
        getChildren().add(titleLabel);

        String description = task.getDescription();
        if (description != null && !description.isBlank()) {
            Label descLabel = new Label(truncate(description, DESCRIPTION_PREVIEW_LIMIT));
            descLabel.getStyleClass().add("task-description");
            descLabel.setWrapText(true);
            getChildren().add(descLabel);
        }

        Label dateLabel = new Label(DATE_FORMAT.format(task.getUpdatedAt()));
        dateLabel.getStyleClass().add("task-date");
        getChildren().add(dateLabel);

        getChildren().add(buildActionBar(task, onMovePrevious, onMoveNext, onEdit, onDelete));
    }

    private HBox buildActionBar(Task task, Consumer<Task> onMovePrevious, Consumer<Task> onMoveNext,
                                 Consumer<Task> onEdit, Consumer<Task> onDelete) {
        HBox bar = new HBox(4);
        bar.getStyleClass().add("card-actions");
        // Swallow clicks here so they don't bubble up and also open the
        // detail view behind whatever action button was pressed.
        bar.setOnMouseClicked(Event::consume);

        Button previous = smallButton("‹ Move");
        previous.setDisable(task.getStatus() == TaskStatus.TODO);
        previous.setOnAction(e -> onMovePrevious.accept(task));
        Tooltip.install(previous, new Tooltip("Move to previous column"));

        Button next = smallButton("Move ›");
        next.setDisable(task.getStatus() == TaskStatus.COMPLETED);
        next.setOnAction(e -> onMoveNext.accept(task));
        Tooltip.install(next, new Tooltip("Move to next column"));

        Button edit = smallButton("Edit");
        edit.setOnAction(e -> onEdit.accept(task));

        Button delete = smallButton("Delete");
        delete.getStyleClass().add("btn-small-danger");
        delete.setOnAction(e -> onDelete.accept(task));

        bar.getChildren().addAll(previous, next, edit, delete);
        return bar;
    }

    private static Button smallButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("btn-small");
        return button;
    }

    private static String truncate(String text, int limit) {
        if (text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit).stripTrailing() + "…";
    }
}
