plugins {
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("com.kanban.cli.KanbanCli")
}

// Bundle the :core classes directly into this jar so the CLI ships as a
// single self-contained "kanban-api.jar" runnable with `java -jar`.
tasks.jar {
    dependsOn(":core:jar")
    archiveFileName.set("kanban-api.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.kanban.cli.KanbanCli"
    }
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
}