package com.kanban.core;

import java.io.File;

/**
 * Resolves where the XML database lives, following each OS's usual
 * per-user data directory convention instead of always using the home
 * directory directly.
 */
public final class KanbanPaths {

    private KanbanPaths() {
    }

    public static File dataDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            File base = (appData != null && !appData.isBlank()) ? new File(appData) : new File(home);
            return new File(base, "Kanban");
        }

        if (os.contains("mac")) {
            return new File(home, "Library/Application Support/Kanban");
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        File base = (xdgDataHome != null && !xdgDataHome.isBlank())
                ? new File(xdgDataHome)
                : new File(home, ".local/share");
        return new File(base, "kanban");
    }

    public static File databaseFile() {
        return new File(dataDirectory(), "kanban.xml");
    }

    public static File lockFile() {
        return new File(dataDirectory(), "kanban.xml.lock");
    }
}