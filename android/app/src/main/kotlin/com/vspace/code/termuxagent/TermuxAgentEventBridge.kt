package com.vspace.code.termuxagent

import io.flutter.plugin.common.EventChannel
import java.util.concurrent.ConcurrentHashMap

open class TermuxAgentEventBridge : EventChannel.StreamHandler {
    private val sinks = ConcurrentHashMap<String, EventChannel.EventSink>()

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        val sessionId = extractSessionId(arguments)
        if (sessionId == null) {
            val details = mapOf(
                "code" to AgentErrorCode.VALIDATION.wireValue,
                "message" to "Event subscription requires a non-empty sessionId",
                "details" to mapOf("arguments" to arguments?.toString()),
            )
            events.error(
                AgentErrorCode.VALIDATION.wireValue,
                "Event subscription requires a non-empty sessionId",
                details,
            )
            return
        }

        sinks[sessionId] = events
    }

    override fun onCancel(arguments: Any?) {
        val sessionId = extractSessionId(arguments)
        if (sessionId != null) {
            sinks.remove(sessionId)
        }
    }

    fun unsubscribe(sessionId: String) {
        sinks.remove(sessionId)
    }

    fun clearAll() {
        sinks.clear()
    }

    @Synchronized
    open fun emit(sessionId: String, payload: Map<String, Any?>) {
        sinks[sessionId]?.success(payload)
    }

    private fun extractSessionId(arguments: Any?): String? {
        if (arguments !is Map<*, *>) return null
        val value = arguments["sessionId"] as? String ?: return null
        return value.takeIf { it.isNotBlank() }
    }
}
