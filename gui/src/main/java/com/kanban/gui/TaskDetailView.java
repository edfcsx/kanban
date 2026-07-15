package com.kanban.gui;

import com.kanban.core.Task;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Window;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Read-only "attributes" view for a single task, opened by clicking its
 * card. Description is rendered as markdown so embedded code snippets show
 * up formatted rather than as raw ``` fences.
 */
final class TaskDetailView {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private TaskDetailView() {
    }

    static void show(Window owner, Task task, Consumer<Task> onEdit) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(task.getTitle());
        dialog.getDialogPane().getStylesheets().add(Styles.SHEET);
        dialog.getDialogPane().getStyleClass().add("task-detail-pane");

        ButtonType editButtonType = new ButtonType("Edit", ButtonBar.ButtonData.OTHER);
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(editButtonType, closeButtonType);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("detail-title");
        titleLabel.setWrapText(true);

        HBox badgeRow = new HBox(6, Badges.status(task.getStatus()), Badges.category(task.getCategory()));

        Label metaLabel = new Label("Created " + DATE_FORMAT.format(task.getCreatedAt())
                + "   ·   Updated " + DATE_FORMAT.format(task.getUpdatedAt()));
        metaLabel.getStyleClass().add("detail-meta");

        WebView descriptionView = new WebView();
        descriptionView.getStyleClass().add("detail-web-frame");
        descriptionView.setPrefSize(480, 320);
        descriptionView.getEngine().loadContent(MarkdownRenderer.toHtml(task.getDescription()));

        VBox content = new VBox(8, titleLabel, badgeRow, metaLabel, descriptionView);
        content.setPadding(new Insets(4, 0, 0, 0));

        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == editButtonType) {
            onEdit.accept(task);
        }
    }
}
