package com.vspace.code.termuxagent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WorkspaceRepositoryTest {

    @Test
    fun createAndListWorkspaces_persistsToDisk() {
        val baseDir = Files.createTempDirectory("workspace_repo_test").toFile()
        val repository = WorkspaceRepository(baseDir)

        val created = repository.createWorkspace("demo", "stub-v1")
        val listed = repository.listWorkspaces()

        assertEquals(1, listed.size)
        assertEquals(created.id, listed.first().id)
        assertEquals("ready", listed.first().state)

        val repositoryReloaded = WorkspaceRepository(baseDir)
        val reloaded = repositoryReloaded.listWorkspaces()
        assertEquals(1, reloaded.size)
        assertEquals(created.id, reloaded.first().id)
    }

    @Test
    fun importAndExport_fileModeAndDestination() {
        val baseDir = Files.createTempDirectory("workspace_import_export_test").toFile()
        val repository = WorkspaceRepository(baseDir)
        val workspace = repository.createWorkspace("demo", "stub-v1")

        val sourceFile = File(baseDir, "source.txt")
        sourceFile.writeText("hello")

        val importedPath = repository.importIntoWorkspace(
            workspaceId = workspace.id,
            sourceUri = sourceFile.absolutePath,
            mode = "file",
        )
        val importedFile = File(importedPath)
        assertTrue(importedFile.exists())
        assertEquals("hello", importedFile.readText())

        val exportDir = File(baseDir, "export")
        val exportedUri = repository.exportWorkspace(workspace.id, exportDir.absolutePath)
        assertTrue(exportedUri.startsWith("file://"))

        val exportedFile = File(exportDir, "imports/${sourceFile.name}")
        assertTrue(exportedFile.exists())
        assertEquals("hello", exportedFile.readText())
    }
}
