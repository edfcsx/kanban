package com.kanban.gui;

import com.kanban.core.KanbanRepository;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class KanbanApp extends Application {

    @Override
    public void start(Stage stage) {
        MainView root = new MainView(new KanbanRepository());

        Scene scene = new Scene(root, 1080, 680);
        scene.getStylesheets().add(Styles.SHEET);

        stage.setTitle("Kanban");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.show();
    }
}
