package com.kanban.gui;

import javafx.application.Application;

/**
 * A plain (non-Application) entry point. Running an Application subclass
 * directly as a fat jar's Main-Class trips the "JavaFX runtime components
 * are missing" check on some setups, so this indirection launches it instead.
 */
public final class Launcher {

    public static void main(String[] args) {
        Application.launch(KanbanApp.class, args);
    }

    private Launcher() {
    }
}
