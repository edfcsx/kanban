package com.kanban.gui;

import com.kanban.core.KanbanRepository;
import com.kanban.core.Project;
import com.kanban.core.ProjectRegistry;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Startup screen: pick an existing project to open, or create a new one.
 * Shown before the board so every task is always scoped to a project.
 */
final class ProjectPickerView extends BorderPane {

    ProjectPickerView(ProjectRegistry registry, Consumer<Project> onProjectChosen) {
        getStyleClass().add("picker-root");

        Label title = new Label("Kanban");
        title.getStyleClass().add("picker-title");

        Label subtitle = new Label("Choose a project to open, or create a new one");
        subtitle.getStyleClass().add("picker-subtitle");

        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(32, 0, 20, 0));
        header.setAlignment(Pos.CENTER);

        VBox projectList = new VBox(8);
        List<Project> projects = registry.listProjects();
        if (projects.isEmpty()) {
            Label empty = new Label("No projects yet - create your first one below.");
            empty.getStyleClass().add("form-hint");
            projectList.getChildren().add(empty);
        } else {
            for (Project project : projects) {
                projectList.getChildren().add(buildProjectRow(registry, project, onProjectChosen));
            }
        }

        ScrollPane scrollPane = new ScrollPane(projectList);
        scrollPane.getStyleClass().add("column-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(280);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        TextField nameField = new TextField();
        nameField.getStyleClass().add("form-text-field");
        nameField.setPromptText("New project name");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        Button createButton = new Button("+ Create");
        createButton.getStyleClass().add("btn-primary");
        Runnable create = () -> {
            if (nameField.getText().isBlank()) {
                return;
            }
            onProjectChosen.accept(registry.createProject(nameField.getText().trim()));
        };
        createButton.setOnAction(e -> create.run());
        nameField.setOnAction(e -> create.run());

        HBox createRow = new HBox(8, nameField, createButton);
        createRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(16, scrollPane, createRow);
        content.getStyleClass().add("picker-panel");
        content.setMaxWidth(480);
        content.setPadding(new Insets(24));

        setTop(header);
        setCenter(content);
        BorderPane.setAlignment(content, Pos.CENTER);
        BorderPane.setMargin(content, new Insets(0, 0, 40, 0));
    }

    private HBox buildProjectRow(ProjectRegistry registry, Project project, Consumer<Project> onProjectChosen) {
        int taskCount = new KanbanRepository(registry.taskDbFile(project), registry.taskLockFile(project))
                .loadAll().size();

        Label name = new Label(project.displayName());
        name.getStyleClass().add("task-title");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label count = new Label(taskCount + (taskCount == 1 ? " task" : " tasks"));
        count.getStyleClass().add("detail-meta");

        HBox row = new HBox(12, name, count);
        row.getStyleClass().add("task-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> onProjectChosen.accept(project));
        return row;
    }
}
