package com.kanban.gui;

import java.util.Objects;

/** Shared handle to the app's stylesheet, since each Dialog gets its own Scene. */
final class Styles {

    static final String SHEET = Objects.requireNonNull(Styles.class.getResource("kanban.css")).toExternalForm();

    private Styles() {
    }
}
