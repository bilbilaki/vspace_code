package com.vspace.code.termuxagent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
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

    @Test
    fun export_replacesDestinationSnapshotAndRemovesStaleFiles() {
        val baseDir = Files.createTempDirectory("workspace_export_snapshot_test").toFile()
        val repository = WorkspaceRepository(baseDir)
        val workspace = repository.createWorkspace("demo", "stub-v1")
        val workspaceDir = repository.getWorkspaceDir(workspace.id)
        val projectDir = File(workspaceDir, "project")

        File(projectDir, "fresh.txt").writeText("new")

        val exportDir = File(baseDir, "export")
        exportDir.mkdirs()
        File(exportDir, "stale.txt").writeText("old")

        repository.exportWorkspace(workspace.id, exportDir.absolutePath)

        assertTrue(File(exportDir, "fresh.txt").exists())
        assertEquals("new", File(exportDir, "fresh.txt").readText())
        assertTrue(!File(exportDir, "stale.txt").exists())
    }

    @Test
    fun import_symlinkSource_isRejected() {
        val baseDir = Files.createTempDirectory("workspace_import_symlink_test").toFile()
        val repository = WorkspaceRepository(baseDir)
        val workspace = repository.createWorkspace("demo", "stub-v1")

        val realSource = File(baseDir, "real.txt")
        realSource.writeText("hello")
        val symlink = File(baseDir, "symlink.txt").toPath()

        val symlinkSupported = try {
            Files.createSymbolicLink(symlink, realSource.toPath())
            true
        } catch (_: Throwable) {
            false
        }
        Assume.assumeTrue("Symlink creation unsupported on this environment", symlinkSupported)

        val error = runCatching {
            repository.importIntoWorkspace(workspace.id, symlink.toFile().absolutePath, "file")
        }.exceptionOrNull()

        assertTrue(error is TermuxAgentException)
        val domainError = error as TermuxAgentException
        assertEquals(AgentErrorCode.VALIDATION, domainError.code)
    }

    @Test
    fun export_symlinkInsideProject_isRejected() {
        val baseDir = Files.createTempDirectory("workspace_export_symlink_test").toFile()
        val repository = WorkspaceRepository(baseDir)
        val workspace = repository.createWorkspace("demo", "stub-v1")
        val workspaceDir = repository.getWorkspaceDir(workspace.id)
        val projectDir = File(workspaceDir, "project")

        val realSource = File(baseDir, "real.txt")
        realSource.writeText("hello")
        val symlink = File(projectDir, "linked.txt").toPath()

        val symlinkSupported = try {
            Files.createSymbolicLink(symlink, realSource.toPath())
            true
        } catch (_: Throwable) {
            false
        }
        Assume.assumeTrue("Symlink creation unsupported on this environment", symlinkSupported)

        val exportDir = File(baseDir, "export")
        val error = runCatching {
            repository.exportWorkspace(workspace.id, exportDir.absolutePath)
        }.exceptionOrNull()

        assertTrue(error is TermuxAgentException)
        val domainError = error as TermuxAgentException
        assertEquals(AgentErrorCode.VALIDATION, domainError.code)
    }
}
