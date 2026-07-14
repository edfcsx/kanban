package com.kanban.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KanbanRepositoryTest {

    @Test
    void addListUpdateAndDeleteTaskRoundTrip(@TempDir File tempDir) {
        KanbanRepository repo = new KanbanRepository(
                new File(tempDir, "kanban.xml"),
                new File(tempDir, "kanban.xml.lock"));

        Task created = repo.addTask("Write tests", "Cover the repository", TaskCategory.CHORE);
        assertEquals(TaskStatus.TODO, created.getStatus());
        assertEquals(TaskCategory.CHORE, created.getCategory());

        List<Task> all = repo.loadAll();
        assertEquals(1, all.size());
        assertEquals("Write tests", all.get(0).getTitle());
        assertEquals(TaskCategory.CHORE, all.get(0).getCategory());

        assertTrue(repo.updateStatus(created.getId(), TaskStatus.IN_PROGRESS));
        assertEquals(TaskStatus.IN_PROGRESS, repo.findById(created.getId()).getStatus());

        assertTrue(repo.updateTask(created.getId(), "Write more tests", null));
        assertEquals("Write more tests", repo.findById(created.getId()).getTitle());
        assertEquals("Cover the repository", repo.findById(created.getId()).getDescription());
        assertEquals(TaskCategory.CHORE, repo.findById(created.getId()).getCategory());

        assertTrue(repo.updateTask(created.getId(), null, null, TaskCategory.BUG));
        assertEquals(TaskCategory.BUG, repo.findById(created.getId()).getCategory());

        assertTrue(repo.deleteTask(created.getId()));
        assertTrue(repo.loadAll().isEmpty());
        assertFalse(repo.deleteTask(created.getId()));
    }

    @Test
    void persistsAcrossRepositoryInstances(@TempDir File tempDir) {
        File db = new File(tempDir, "kanban.xml");
        File lock = new File(tempDir, "kanban.xml.lock");

        new KanbanRepository(db, lock).addTask("Persisted task", "desc");

        KanbanRepository reopened = new KanbanRepository(db, lock);
        List<Task> tasks = reopened.loadAll();
        assertEquals(1, tasks.size());
        assertEquals("Persisted task", tasks.get(0).getTitle());
    }

    @Test
    void missingCategoryAttributeDefaultsToNone(@TempDir File tempDir) throws Exception {
        File db = new File(tempDir, "kanban.xml");
        java.nio.file.Files.writeString(db.toPath(), """
                <kanban>
                  <task id="legacy-1" status="TODO" createdAt="2024-01-01T00:00:00Z" updatedAt="2024-01-01T00:00:00Z">
                    <title>Old task</title>
                    <description>No category attribute</description>
                  </task>
                </kanban>
                """);

        KanbanRepository repo = new KanbanRepository(db, new File(tempDir, "kanban.xml.lock"));
        List<Task> tasks = repo.loadAll();
        assertEquals(1, tasks.size());
        assertEquals(TaskCategory.NONE, tasks.get(0).getCategory());
    }

    @Test
    void statusParsingAcceptsCommonAliases() {
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.parse("in-progress"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.parse("Doing"));
        assertEquals(TaskStatus.COMPLETED, TaskStatus.parse("done"));
        assertEquals(TaskStatus.TODO, TaskStatus.parse("ToDo"));
    }
}