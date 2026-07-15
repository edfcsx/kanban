package com.kanban.gui;

import com.kanban.core.TaskCategory;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Window;

import java.util.Optional;

/** Modal form used for both creating and editing a task. */
final class TaskDialog {

    record Result(String title, String description, TaskCategory category) {
    }

    private TaskDialog() {
    }

    static Result show(Window owner, String dialogTitle, String initialTitle, String initialDescription,
                        TaskCategory initialCategory) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(dialogTitle);
        dialog.getDialogPane().getStylesheets().add(Styles.SHEET);
        dialog.getDialogPane().getStyleClass().add("task-dialog-pane");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField(initialTitle == null ? "" : initialTitle);
        titleField.getStyleClass().add("form-text-field");

        ComboBox<TaskCategory> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(TaskCategory.values()));
        categoryCombo.getStyleClass().add("form-combo");
        categoryCombo.setValue(initialCategory == null ? TaskCategory.NONE : initialCategory);
        categoryCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TaskCategory category) {
                return category == null ? "" : category.label();
            }

            @Override
            public TaskCategory fromString(String string) {
                return null;
            }
        });

        TextArea descriptionArea = new TextArea(initialDescription == null ? "" : initialDescription);
        descriptionArea.getStyleClass().add("form-text-area");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(10);
        descriptionArea.setStyle("-fx-font-family: 'Cascadia Code', Consolas, monospace;");

        WebView previewView = new WebView();
        previewView.getStyleClass().add("detail-web-frame");

        TabPane descriptionTabs = new TabPane();
        descriptionTabs.getStyleClass().add("description-tabs");
        descriptionTabs.setPrefSize(440, 240);
        Tab writeTab = new Tab("Write", descriptionArea);
        writeTab.setClosable(false);
        Tab previewTab = new Tab("Preview", previewView);
        previewTab.setClosable(false);
        descriptionTabs.getTabs().addAll(writeTab, previewTab);
        descriptionTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == previewTab) {
                previewView.getEngine().loadContent(MarkdownRenderer.toHtml(descriptionArea.getText()));
            }
        });

        HBox categoryRow = new HBox(8);
        Label categoryLabel = new Label("Category");
        categoryLabel.getStyleClass().add("form-label");
        categoryRow.getChildren().addAll(categoryLabel, categoryCombo);

        Label titleLabel = new Label("Title");
        titleLabel.getStyleClass().add("form-label");

        Label descLabel = new Label("Description (Markdown supported, e.g. ```code``` for snippets)");
        descLabel.getStyleClass().add("form-hint");

        VBox form = new VBox(8, titleLabel, titleField, categoryRow, descLabel, descriptionTabs);
        form.setPadding(new Insets(4, 0, 0, 0));

        dialog.getDialogPane().setContent(form);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(titleField.getText().isBlank());
        titleField.textProperty().addListener((obs, oldVal, newVal) -> okButton.setDisable(newVal.isBlank()));

        dialog.setOnShown(e -> Platform.runLater(titleField::requestFocus));

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return new Result(titleField.getText().trim(), descriptionArea.getText(), categoryCombo.getValue());
            }
            return null;
        });

        Optional<Result> result = dialog.showAndWait();
        return result.orElse(null);
    }
}
