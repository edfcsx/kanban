plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(platform("io.modelcontextprotocol.sdk:mcp-bom:2.0.0"))
    implementation("io.modelcontextprotocol.sdk:mcp")
}

application {
    mainClass.set("com.kanban.mcp.KanbanMcpServer")
}

// Bundle the :core classes and the MCP SDK directly into this jar so it
// ships as a single self-contained "kanban-mcp.jar" runnable with `java -jar`.
tasks.jar {
    dependsOn(":core:jar")
    archiveFileName.set("kanban-mcp.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.kanban.mcp.KanbanMcpServer"
    }
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
}
