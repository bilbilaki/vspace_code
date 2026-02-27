package com.vspace.code.termuxagent

import java.util.concurrent.ConcurrentHashMap

data class SessionRecord(
    val sessionId: String,
    val workspaceId: String,
    val sessionKind: String,
    var state: String,
    var active: Boolean = true,
    var chunkIndex: Int = 0,
)

class SessionRegistry {
    private val sessions = ConcurrentHashMap<String, SessionRecord>()

    fun create(sessionId: String, workspaceId: String, sessionKind: String, state: String): SessionRecord {
        val record = SessionRecord(
            sessionId = sessionId,
            workspaceId = workspaceId,
            sessionKind = sessionKind,
            state = state,
            active = true,
            chunkIndex = 0,
        )
        sessions[sessionId] = record
        return record
    }

    fun get(sessionId: String): SessionRecord? = sessions[sessionId]

    fun requireActive(sessionId: String): SessionRecord {
        val session = sessions[sessionId]
            ?: throw TermuxAgentException(
                AgentErrorCode.PROCESS,
                "Unknown sessionId: $sessionId",
            )
        if (!session.active) {
            throw TermuxAgentException(
                AgentErrorCode.PROCESS,
                "Session is not active: $sessionId",
            )
        }
        return session
    }

    fun updateState(sessionId: String, state: String) {
        sessions[sessionId]?.state = state
    }

    fun nextChunkIndex(sessionId: String): Int {
        val session = requireActive(sessionId)
        val next = session.chunkIndex
        session.chunkIndex += 1
        return next
    }

    fun listByWorkspace(workspaceId: String): List<SessionRecord> {
        return sessions.values.filter { it.workspaceId == workspaceId && it.active }
    }

    fun deactivate(sessionId: String) {
        sessions[sessionId]?.let {
            it.active = false
            it.state = "stopped"
        }
    }

    fun remove(sessionId: String) {
        sessions.remove(sessionId)
    }
}
