package com.kanban.gui;

import com.kanban.core.KanbanRepository;
import com.kanban.core.Project;
import com.kanban.core.ProjectRegistry;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class KanbanApp extends Application {

    private final ProjectRegistry registry = new ProjectRegistry();

    @Override
    public void start(Stage stage) {
        registry.migrateLegacyDatabaseIfNeeded();

        Scene scene = new Scene(buildPicker(stage), 1080, 680);
        scene.getStylesheets().add(Styles.SHEET);

        stage.setTitle("Kanban");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.show();
    }

    private Parent buildPicker(Stage stage) {
        return new ProjectPickerView(registry, project -> openProject(stage, project));
    }

    private void openProject(Stage stage, Project project) {
        registry.setCurrentProject(project);
        KanbanRepository repository = new KanbanRepository(
                registry.taskDbFile(project), registry.taskLockFile(project));
        MainView board = new MainView(repository, project, () -> stage.getScene().setRoot(buildPicker(stage)));
        stage.getScene().setRoot(board);
    }
}
