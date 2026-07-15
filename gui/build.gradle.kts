plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    version = "21.0.11"
    modules = listOf("javafx.controls", "javafx.web")
    // Fixed to Linux: install.sh only targets Linux, and the fat jar can
    // only bundle one platform's native libs at a time.
    setPlatform("linux")
}

dependencies {
    implementation(project(":core"))
    implementation("org.commonmark:commonmark:0.29.0")
}

application {
    // JavaFX's own Application subclass can't be the jar's Main-Class in a
    // non-modular fat jar (triggers "JavaFX runtime components missing"),
    // so a plain launcher class calls Application.launch() instead.
    mainClass.set("com.kanban.gui.Launcher")
}

// Bundle the :core classes directly into this jar so the app ships as a
// single self-contained "kanban-gui.jar" runnable with `java -jar`.
tasks.jar {
    dependsOn(":core:jar")
    archiveFileName.set("kanban-gui.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.kanban.gui.Launcher"
    }
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
}