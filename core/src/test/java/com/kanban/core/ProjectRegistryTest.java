package com.kanban.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectRegistryTest {

    private static ProjectRegistry registryIn(File tempDir) {
        return new ProjectRegistry(
                new File(tempDir, "projects"),
                new File(tempDir, "state.properties"),
                new File(tempDir, "kanban.xml"),
                new File(tempDir, "kanban.xml.lock"));
    }

    @Test
    void createProjectGeneratesSlugAndBecomesCurrent(@TempDir File tempDir) {
        ProjectRegistry registry = registryIn(tempDir);

        Project project = registry.createProject("Kanban App");
        assertEquals("kanban-app", project.slug());
        assertEquals("Kanban App", project.displayName());
        assertEquals(Optional.of(project), registry.currentProject());
        assertEquals(List.of(project), registry.listProjects());
    }

    @Test
    void duplicateNamesGetSuffixedSlugs(@TempDir File tempDir) {
        ProjectRegistry registry = registryIn(tempDir);

        Project first = registry.createProject("Backend");
        Project second = registry.createProject("Backend");

        assertEquals("backend", first.slug());
        assertEquals("backend-2", second.slug());
        assertEquals(2, registry.listProjects().size());
    }

    @Test
    void currentProjectIsEmptyWhenNoneSelectedOrSlugIsStale(@TempDir File tempDir) {
        ProjectRegistry registry = registryIn(tempDir);
        assertEquals(Optional.empty(), registry.currentProject());

        Project project = registry.createProject("Temp");
        assertEquals(Optional.of(project), registry.currentProject());
    }

    @Test
    void migratesLegacyDatabaseIntoDefaultProjectOnFirstRun(@TempDir File tempDir) throws Exception {
        File legacyDb = new File(tempDir, "kanban.xml");
        Files.writeString(legacyDb.toPath(), "<kanban><task id=\"1\" status=\"TODO\" "
                + "createdAt=\"2024-01-01T00:00:00Z\" updatedAt=\"2024-01-01T00:00:00Z\">"
                + "<title>Legacy task</title><description></description></task></kanban>");
        File legacyLock = new File(tempDir, "kanban.xml.lock");
        Files.writeString(legacyLock.toPath(), "");

        ProjectRegistry registry = registryIn(tempDir);
        Optional<Project> migrated = registry.migrateLegacyDatabaseIfNeeded();

        assertTrue(migrated.isPresent());
        assertEquals("Default", migrated.get().displayName());
        assertFalse(legacyDb.exists());
        assertTrue(registry.taskDbFile(migrated.get()).exists());
        assertEquals(Optional.of(migrated.get()), registry.currentProject());

        KanbanRepository repo = new KanbanRepository(
                registry.taskDbFile(migrated.get()), registry.taskLockFile(migrated.get()));
        assertEquals(1, repo.loadAll().size());
        assertEquals("Legacy task", repo.loadAll().get(0).getTitle());
    }

    @Test
    void migrationIsNoOpWhenProjectsAlreadyExistOrNoLegacyFile(@TempDir File tempDir) {
        ProjectRegistry registry = registryIn(tempDir);
        assertEquals(Optional.empty(), registry.migrateLegacyDatabaseIfNeeded());

        registry.createProject("Already Set Up");
        assertEquals(Optional.empty(), registry.migrateLegacyDatabaseIfNeeded());
    }
}
