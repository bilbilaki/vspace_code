package com.vspace.code.termuxagent

import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class StartSessionResult(
    val sessionId: String,
    val state: String,
)

class StubSessionEngine(
    private val sessionRegistry: SessionRegistry,
    private val eventBridge: TermuxAgentEventBridge,
) {
    private val random = SecureRandom()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun startSession(workspaceId: String, sessionKind: String): StartSessionResult {
        val sessionId = generateSessionId()
        sessionRegistry.create(
            sessionId = sessionId,
            workspaceId = workspaceId,
            sessionKind = sessionKind,
            state = "running",
        )

        executor.execute {
            emitState(sessionId, "created")
            emitState(sessionId, "running")
        }

        return StartSessionResult(sessionId = sessionId, state = "running")
    }

    fun writeSession(sessionId: String, bytesBase64: String): Int {
        sessionRegistry.requireActive(sessionId)

        val bytes = try {
            Base64.getDecoder().decode(bytesBase64)
        } catch (_: IllegalArgumentException) {
            throw TermuxAgentException(
                AgentErrorCode.VALIDATION,
                "Invalid base64 payload in writeSession",
            )
        }

        val acceptedBytes = bytes.size
        val chunkBase64 = Base64.getEncoder().encodeToString(bytes)
        val chunkIndex = sessionRegistry.nextChunkIndex(sessionId)

        executor.execute {
            val payload = baseEvent(sessionId, "stdout").toMutableMap()
            payload["chunkBase64"] = chunkBase64
            payload["chunkIndex"] = chunkIndex
            eventBridge.emit(sessionId, payload)
        }

        return acceptedBytes
    }

    fun stopSession(sessionId: String, signal: String?): Boolean {
        sessionRegistry.requireActive(sessionId)

        sessionRegistry.updateState(sessionId, "stopping")
        executor.execute {
            emitState(sessionId, "stopping")
            emitState(sessionId, "stopped")

            val exitEvent = baseEvent(sessionId, "exit").toMutableMap()
            exitEvent["exit"] = mapOf(
                "code" to 0,
                "signal" to signal,
                "source" to "stub",
            )
            eventBridge.emit(sessionId, exitEvent)

            sessionRegistry.deactivate(sessionId)
            sessionRegistry.remove(sessionId)
            eventBridge.unsubscribe(sessionId)
        }

        return true
    }

    fun stopSessionsForWorkspace(workspaceId: String) {
        sessionRegistry.listByWorkspace(workspaceId).forEach { session ->
            stopSession(session.sessionId, "TERM")
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun emitState(sessionId: String, state: String) {
        val payload = baseEvent(sessionId, "state").toMutableMap()
        payload["state"] = state
        eventBridge.emit(sessionId, payload)
    }

    private fun baseEvent(sessionId: String, type: String): Map<String, Any?> {
        return mapOf(
            "sessionId" to sessionId,
            "type" to type,
            "timestamp" to utcNowIso(),
        )
    }

    private fun generateSessionId(): String {
        val suffix = random.nextInt(0x10000).toString(16).padStart(4, '0')
        return "ss_${System.currentTimeMillis()}_$suffix"
    }

    private fun utcNowIso(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }
}
