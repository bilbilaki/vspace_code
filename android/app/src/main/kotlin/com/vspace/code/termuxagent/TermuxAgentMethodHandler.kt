package com.vspace.code.termuxagent

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class TermuxAgentMethodHandler(
    private val runtimeRepository: RuntimeRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val sessionRegistry: SessionRegistry,
    private val sessionEngine: StubSessionEngine,
    private val eventBridge: TermuxAgentEventBridge,
) : MethodChannel.MethodCallHandler {

    companion object {
        val SUPPORTED_METHODS: Set<String> = setOf(
            "initializeRuntime",
            "listWorkspaces",
            "createWorkspace",
            "deleteWorkspace",
            "importIntoWorkspace",
            "exportWorkspace",
            "startSession",
            "writeSession",
            "stopSession",
            "unsubscribeSessionEvents",
        )

        private val SUPPORTED_SESSION_KINDS: Set<String> = setOf("shell", "lsp", "pty-terminal")
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "initializeRuntime" -> handleInitializeRuntime(call, result)
                "listWorkspaces" -> handleListWorkspaces(result)
                "createWorkspace" -> handleCreateWorkspace(call, result)
                "deleteWorkspace" -> handleDeleteWorkspace(call, result)
                "importIntoWorkspace" -> handleImportIntoWorkspace(call, result)
                "exportWorkspace" -> handleExportWorkspace(call, result)
                "startSession" -> handleStartSession(call, result)
                "writeSession" -> handleWriteSession(call, result)
                "stopSession" -> handleStopSession(call, result)
                "unsubscribeSessionEvents" -> handleUnsubscribeSessionEvents(call, result)
                else -> result.notImplemented()
            }
        } catch (error: TermuxAgentException) {
            reportError(result, error)
        } catch (error: Throwable) {
            reportError(
                result,
                TermuxAgentException(
                    code = AgentErrorCode.UNKNOWN,
                    message = error.message ?: "Unknown host error",
                    details = mapOf("exception" to error::class.java.simpleName),
                )
            )
        }
    }

    private fun handleInitializeRuntime(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val forceRecreate = readBoolean(args, "forceRecreate", defaultValue = false)
        val runtimeVersion = readOptionalString(args, "runtimeVersion")

        val response = runtimeRepository.initializeRuntime(forceRecreate, runtimeVersion)
        result.success(
            mapOf(
                "runtimeVersion" to response.runtimeVersion,
                "runtimeRootPath" to response.runtimeRootPath,
                "installedNow" to response.installedNow,
            )
        )
    }

    private fun handleListWorkspaces(result: MethodChannel.Result) {
        val workspaces = workspaceRepository.listWorkspaces().map { descriptor ->
            mapOf(
                "id" to descriptor.id,
                "name" to descriptor.name,
                "createdAt" to descriptor.createdAt,
                "lastUsedAt" to descriptor.lastUsedAt,
                "state" to descriptor.state,
                "runtimeVersion" to descriptor.runtimeVersion,
            )
        }

        result.success(mapOf("workspaces" to workspaces))
    }

    private fun handleCreateWorkspace(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val name = readRequiredString(args, "name")
        val runtimeVersion = "stub-v1"

        val workspace = workspaceRepository.createWorkspace(name, runtimeVersion)
        result.success(
            mapOf(
                "workspace" to mapOf(
                    "id" to workspace.id,
                    "name" to workspace.name,
                    "createdAt" to workspace.createdAt,
                    "lastUsedAt" to workspace.lastUsedAt,
                    "state" to workspace.state,
                    "runtimeVersion" to workspace.runtimeVersion,
                )
            )
        )
    }

    private fun handleDeleteWorkspace(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val workspaceId = readRequiredString(args, "workspaceId")

        sessionEngine.stopSessionsForWorkspace(workspaceId)
        val deleted = workspaceRepository.deleteWorkspace(workspaceId)

        result.success(mapOf("deleted" to deleted))
    }

    private fun handleImportIntoWorkspace(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val workspaceId = readRequiredString(args, "workspaceId")
        val sourceUri = readRequiredString(args, "sourceUri")
        val mode = readRequiredString(args, "mode")

        if (!workspaceRepository.workspaceExists(workspaceId)) {
            throw TermuxAgentException(
                AgentErrorCode.WORKSPACE,
                "Unknown workspaceId: $workspaceId",
            )
        }

        val importedPath = workspaceRepository.importIntoWorkspace(workspaceId, sourceUri, mode)
        result.success(mapOf("importedPath" to importedPath))
    }

    private fun handleExportWorkspace(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val workspaceId = readRequiredString(args, "workspaceId")
        val destinationUri = readRequiredString(args, "destinationUri")

        if (!workspaceRepository.workspaceExists(workspaceId)) {
            throw TermuxAgentException(
                AgentErrorCode.WORKSPACE,
                "Unknown workspaceId: $workspaceId",
            )
        }

        val exportedUri = workspaceRepository.exportWorkspace(workspaceId, destinationUri)
        result.success(mapOf("exportedUri" to exportedUri))
    }

    private fun handleStartSession(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val workspaceId = readRequiredString(args, "workspaceId")
        val sessionKind = readRequiredString(args, "sessionKind")

        if (!workspaceRepository.workspaceExists(workspaceId)) {
            throw TermuxAgentException(
                AgentErrorCode.WORKSPACE,
                "Unknown workspaceId: $workspaceId",
            )
        }

        if (!SUPPORTED_SESSION_KINDS.contains(sessionKind)) {
            throw TermuxAgentException(
                AgentErrorCode.VALIDATION,
                "Unsupported sessionKind: $sessionKind",
            )
        }

        readRequiredString(args, "executable")

        val response = sessionEngine.startSession(workspaceId, sessionKind)
        result.success(
            mapOf(
                "sessionId" to response.sessionId,
                "state" to response.state,
            )
        )
    }

    private fun handleWriteSession(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val sessionId = readRequiredString(args, "sessionId")
        val bytesBase64 = readRequiredString(args, "bytesBase64")

        val acceptedBytes = sessionEngine.writeSession(sessionId, bytesBase64)
        result.success(mapOf("acceptedBytes" to acceptedBytes))
    }

    private fun handleStopSession(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val sessionId = readRequiredString(args, "sessionId")
        val signal = readOptionalString(args, "signal")

        val stopped = sessionEngine.stopSession(sessionId, signal)
        result.success(mapOf("stopped" to stopped))
    }

    private fun handleUnsubscribeSessionEvents(call: MethodCall, result: MethodChannel.Result) {
        val args = argumentsAsMap(call)
        val sessionId = readRequiredString(args, "sessionId")

        eventBridge.unsubscribe(sessionId)
        sessionRegistry.remove(sessionId)
        result.success(null)
    }

    private fun argumentsAsMap(call: MethodCall): Map<*, *> {
        val arguments = call.arguments
        if (arguments == null) return emptyMap<String, Any?>()
        if (arguments is Map<*, *>) return arguments

        throw TermuxAgentException(
            AgentErrorCode.VALIDATION,
            "Arguments for ${call.method} must be a map",
        )
    }

    private fun readRequiredString(args: Map<*, *>, key: String): String {
        val value = args[key] as? String
        if (value.isNullOrBlank()) {
            throw TermuxAgentException(
                AgentErrorCode.VALIDATION,
                "Missing or invalid required string argument: $key",
            )
        }
        return value
    }

    private fun readOptionalString(args: Map<*, *>, key: String): String? {
        val value = args[key]
        if (value == null) return null
        if (value is String) return value
        throw TermuxAgentException(
            AgentErrorCode.VALIDATION,
            "Invalid string argument: $key",
        )
    }

    private fun readBoolean(args: Map<*, *>, key: String, defaultValue: Boolean): Boolean {
        val value = args[key] ?: return defaultValue
        if (value is Boolean) return value
        throw TermuxAgentException(
            AgentErrorCode.VALIDATION,
            "Invalid boolean argument: $key",
        )
    }

    private fun reportError(result: MethodChannel.Result, error: TermuxAgentException) {
        val payload = mapOf(
            "code" to error.code.wireValue,
            "message" to error.message,
            "details" to error.details,
        )
        result.error(error.code.wireValue, error.message, payload)
    }
}
