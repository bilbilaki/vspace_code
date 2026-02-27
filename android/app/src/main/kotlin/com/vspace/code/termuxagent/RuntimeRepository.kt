package com.vspace.code.termuxagent

import java.io.File

data class InitializeRuntimeResult(
    val runtimeVersion: String,
    val runtimeRootPath: String,
    val installedNow: Boolean,
)

class RuntimeRepository(private val filesDir: File) {
    private val agentRootDir = File(filesDir, "termux_agent")
    private val runtimeCurrentDir = File(agentRootDir, "runtime/current")
    private val runtimeVersionFile = File(runtimeCurrentDir, "runtime_version.txt")

    fun initializeRuntime(forceRecreate: Boolean, requestedVersion: String?): InitializeRuntimeResult {
        val defaultVersion = requestedVersion?.ifBlank { null } ?: "stub-v1"

        ensureBaseDirectories()

        var installedNow = false
        if (forceRecreate || !runtimeCurrentDir.exists()) {
            deleteRecursively(runtimeCurrentDir)
            if (!runtimeCurrentDir.mkdirs()) {
                throw TermuxAgentException(
                    AgentErrorCode.BOOTSTRAP,
                    "Failed to create runtime directory: ${runtimeCurrentDir.absolutePath}",
                )
            }
            runtimeVersionFile.writeText(defaultVersion)
            installedNow = true
        }

        if (!runtimeVersionFile.exists()) {
            runtimeVersionFile.writeText(defaultVersion)
            installedNow = true
        }

        val currentVersion = runtimeVersionFile.readText().trim().ifEmpty { defaultVersion }

        return InitializeRuntimeResult(
            runtimeVersion = currentVersion,
            runtimeRootPath = runtimeCurrentDir.absolutePath,
            installedNow = installedNow,
        )
    }

    private fun ensureBaseDirectories() {
        if (!agentRootDir.exists() && !agentRootDir.mkdirs()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to create agent root directory: ${agentRootDir.absolutePath}",
            )
        }

        val metadataDir = File(agentRootDir, "metadata")
        if (!metadataDir.exists() && !metadataDir.mkdirs()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to create metadata directory: ${metadataDir.absolutePath}",
            )
        }

        val workspaceDir = File(agentRootDir, "workspaces")
        if (!workspaceDir.exists() && !workspaceDir.mkdirs()) {
            throw TermuxAgentException(
                AgentErrorCode.IO,
                "Failed to create workspaces directory: ${workspaceDir.absolutePath}",
            )
        }
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
}
