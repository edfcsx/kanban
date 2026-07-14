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

        Task created = repo.addTask("Write tests", "Cover the repository");
        assertEquals(TaskStatus.TODO, created.getStatus());

        List<Task> all = repo.loadAll();
        assertEquals(1, all.size());
        assertEquals("Write tests", all.get(0).getTitle());

        assertTrue(repo.updateStatus(created.getId(), TaskStatus.IN_PROGRESS));
        assertEquals(TaskStatus.IN_PROGRESS, repo.findById(created.getId()).getStatus());

        assertTrue(repo.updateTask(created.getId(), "Write more tests", null));
        assertEquals("Write more tests", repo.findById(created.getId()).getTitle());
        assertEquals("Cover the repository", repo.findById(created.getId()).getDescription());

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
    void statusParsingAcceptsCommonAliases() {
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.parse("in-progress"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.parse("Doing"));
        assertEquals(TaskStatus.COMPLETED, TaskStatus.parse("done"));
        assertEquals(TaskStatus.TODO, TaskStatus.parse("ToDo"));
    }
}