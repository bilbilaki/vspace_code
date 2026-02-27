package com.vspace.code.termuxagent

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class WorkspaceDescriptor(
    val id: String,
    val name: String,
    val createdAt: String,
    val lastUsedAt: String,
    val state: String,
    val runtimeVersion: String,
)

private data class WorkspaceIndex(
    val workspaces: List<WorkspaceDescriptor> = emptyList(),
)

class WorkspaceRepository(private val filesDir: File) {
    private val random = SecureRandom()
    private val gson = Gson()

    private val agentRootDir = File(filesDir, "termux_agent")
    private val workspacesRoot = File(agentRootDir, "workspaces")
    private val metadataDir = File(agentRootDir, "metadata")
    private val metadataFile = File(metadataDir, "workspaces.json")

    fun listWorkspaces(): List<WorkspaceDescriptor> {
        ensurePaths()
        return loadWorkspaces().sortedBy { it.createdAt }
    }

    fun workspaceExists(workspaceId: String): Boolean {
        return loadWorkspaces().any { it.id == workspaceId }
    }

    fun getWorkspaceDir(workspaceId: String): File {
        val workspace = loadWorkspaces().firstOrNull { it.id == workspaceId }
            ?: throw TermuxAgentException(
                AgentErrorCode.WORKSPACE,
                "Unknown workspaceId: $workspaceId",
            )
        return File(workspacesRoot, workspace.id)
    }

    fun createWorkspace(name: String, runtimeVersion: String): WorkspaceDescriptor {
        ensurePaths()
        val safeName = validateWorkspaceName(name)

        val now = utcNowIso()
        val descriptor = WorkspaceDescriptor(
            id = generateWorkspaceId(),
            name = safeName,
            createdAt = now,
            lastUsedAt = now,
            state = "ready",
            runtimeVersion = runtimeVersion,
        )

        val workspaceDir = File(workspacesRoot, descriptor.id)
        createWorkspaceLayout(workspaceDir)

        val current = loadWorkspaces().toMutableList()
        current.add(descriptor)
        saveWorkspaces(current)

        return descriptor
    }

    fun deleteWorkspace(workspaceId: String): Boolean {
        ensurePaths()

        val current = loadWorkspaces().toMutableList()
        val index = current.indexOfFirst { it.id == workspaceId }
        if (index < 0) return false

        val workspaceDir = File(workspacesRoot, workspaceId)
        deleteRecursively(workspaceDir)

        current.removeAt(index)
        saveWorkspaces(current)
        return true
    }

    fun importIntoWorkspace(workspaceId: String, sourceUri: String, mode: String): String {
        val workspaceDir = getWorkspaceDir(workspaceId)
        val source = parseFilePathUri(sourceUri)

        if (!source.exists()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Import source does not exist: ${source.absolutePath}",
            )
        }

        val destinationRoot = File(workspaceDir, "project/imports")
        if (!destinationRoot.exists() && !destinationRoot.mkdirs()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to create import destination: ${destinationRoot.absolutePath}",
            )
        }

        val destination = File(destinationRoot, source.name)

        when (mode) {
            "file" -> {
                if (!source.isFile) {
                    throw TermuxAgentException(
                        AgentErrorCode.VALIDATION,
                        "Import mode 'file' requires a file source",
                    )
                }
                copyEntity(source, destination, overwrite = true, skipExisting = false)
            }
            "folder" -> {
                if (!source.isDirectory) {
                    throw TermuxAgentException(
                        AgentErrorCode.VALIDATION,
                        "Import mode 'folder' requires a directory source",
                    )
                }
                copyEntity(source, destination, overwrite = true, skipExisting = false)
            }
            "merge" -> copyEntity(source, destination, overwrite = true, skipExisting = false)
            "overwrite" -> {
                if (destination.exists()) {
                    deleteRecursively(destination)
                }
                copyEntity(source, destination, overwrite = true, skipExisting = false)
            }
            "skip-existing" -> copyEntity(source, destination, overwrite = false, skipExisting = true)
            else -> throw TermuxAgentException(
                AgentErrorCode.VALIDATION,
                "Unsupported import mode: $mode",
            )
        }

        return destination.absolutePath
    }

    fun exportWorkspace(workspaceId: String, destinationUri: String): String {
        val workspaceDir = getWorkspaceDir(workspaceId)
        val sourceProject = File(workspaceDir, "project")
        if (!sourceProject.exists()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Workspace project directory not found: ${sourceProject.absolutePath}",
            )
        }

        val destination = parseFilePathUri(destinationUri)
        if (destination.exists() && !destination.isDirectory) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Export destination must be a directory: ${destination.absolutePath}",
            )
        }
        if (!destination.exists() && !destination.mkdirs()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to create export destination: ${destination.absolutePath}",
            )
        }

        sourceProject.listFiles()?.forEach { child ->
            copyEntity(
                child,
                File(destination, child.name),
                overwrite = true,
                skipExisting = false,
            )
        }

        return "file://${destination.absolutePath}"
    }

    private fun validateWorkspaceName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw TermuxAgentException(AgentErrorCode.VALIDATION, "Workspace name cannot be empty")
        }
        val valid = Regex("^[A-Za-z0-9._\\- ]{1,64}$").matches(trimmed)
        if (!valid) {
            throw TermuxAgentException(
                AgentErrorCode.VALIDATION,
                "Workspace name contains unsupported characters",
            )
        }
        return trimmed
    }

    private fun generateWorkspaceId(): String {
        val suffix = random.nextInt(0x10000).toString(16).padStart(4, '0')
        return "ws_${System.currentTimeMillis()}_$suffix"
    }

    private fun createWorkspaceLayout(workspaceDir: File) {
        listOf(
            workspaceDir,
            File(workspaceDir, "home"),
            File(workspaceDir, "project"),
            File(workspaceDir, "tmp"),
            File(workspaceDir, ".meta"),
        ).forEach { dir ->
            if (!dir.exists() && !dir.mkdirs()) {
                throw TermuxAgentException(
                    AgentErrorCode.IO,
                    "Failed to create workspace directory: ${dir.absolutePath}",
                )
            }
        }
    }

    private fun ensurePaths() {
        if (!workspacesRoot.exists() && !workspacesRoot.mkdirs()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to create workspaces root: ${workspacesRoot.absolutePath}",
            )
        }
        if (!metadataDir.exists() && !metadataDir.mkdirs()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to create metadata root: ${metadataDir.absolutePath}",
            )
        }
    }

    private fun loadWorkspaces(): List<WorkspaceDescriptor> {
        ensurePaths()
        if (!metadataFile.exists()) return emptyList()

        val raw = metadataFile.readText()
        if (raw.isBlank()) return emptyList()

        val parsed = try {
            gson.fromJson(raw, WorkspaceIndex::class.java)
        } catch (_: JsonSyntaxException) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Invalid workspace metadata format: ${metadataFile.absolutePath}",
            )
        }

        return parsed?.workspaces.orEmpty().sortedBy { it.createdAt }
    }

    private fun saveWorkspaces(workspaces: List<WorkspaceDescriptor>) {
        ensurePaths()

        val root = WorkspaceIndex(workspaces = workspaces.sortedBy { it.createdAt })

        val tempFile = File(metadataDir, "workspaces.json.tmp")
        tempFile.writeText(gson.toJson(root))

        if (metadataFile.exists() && !metadataFile.delete()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to replace metadata file: ${metadataFile.absolutePath}",
            )
        }

        if (!tempFile.renameTo(metadataFile)) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to atomically write workspace metadata",
            )
        }
    }

    private fun parseFilePathUri(raw: String): File {
        if (raw.startsWith("content://")) {
            throw TermuxAgentException(
                AgentErrorCode.PERMISSION,
                "content:// URIs are not supported in this milestone",
            )
        }

        val file = if (raw.startsWith("file://")) {
            File(URI(raw))
        } else {
            File(raw)
        }

        if (!file.isAbsolute) {
            throw TermuxAgentException(
                AgentErrorCode.VALIDATION,
                "Only absolute paths and file:// URIs are supported",
            )
        }

        return file
    }

    private fun copyEntity(source: File, destination: File, overwrite: Boolean, skipExisting: Boolean) {
        if (source.isDirectory) {
            if (destination.exists() && destination.isFile) {
                if (skipExisting) return
                if (!overwrite) {
                    throw TermuxAgentException(
                        AgentErrorCode.IO,
                        "Cannot copy directory over file: ${destination.absolutePath}",
                    )
                }
                deleteRecursively(destination)
            }

            if (!destination.exists() && !destination.mkdirs()) {
                throw TermuxAgentException(
                    AgentErrorCode.IO,
                    "Failed to create destination directory: ${destination.absolutePath}",
                )
            }

            source.listFiles()?.forEach { child ->
                copyEntity(child, File(destination, child.name), overwrite, skipExisting)
            }
            return
        }

        if (source.isFile) {
            if (destination.exists()) {
                if (skipExisting) return
                if (!overwrite) {
                    throw TermuxAgentException(
                        AgentErrorCode.IO,
                        "Destination already exists: ${destination.absolutePath}",
                    )
                }
                deleteRecursively(destination)
            }

            destination.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw TermuxAgentException(
                        AgentErrorCode.IO,
                        "Failed to create destination parent: ${parent.absolutePath}",
                    )
                }
            }

            Files.copy(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
            return
        }

        throw TermuxAgentException(
            AgentErrorCode.IO,
            "Unsupported source type for copy: ${source.absolutePath}",
        )
    }

    private fun deleteRecursively(file: File) {
        if (!file.exists()) return
        file.walkBottomUp().forEach { node ->
            if (!node.delete()) {
                throw TermuxAgentException(
                    AgentErrorCode.IO,
                    "Failed to delete path: ${node.absolutePath}",
                )
            }
        }
    }

    private fun utcNowIso(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }
}
