plugins {
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("com.kanban.gui.KanbanApp")
}

// Bundle the :core classes directly into this jar so the app ships as a
// single self-contained "kanban-gui.jar" runnable with `java -jar`.
tasks.jar {
    dependsOn(":core:jar")
    archiveFileName.set("kanban-gui.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.kanban.gui.KanbanApp"
    }
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
}