package com.vspace.code.termuxagent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Collections

class StubSessionEngineTest {

    @Test
    fun emitsExpectedStateAndExitOrder() {
        val registry = SessionRegistry()
        val bridge = CapturingEventBridge()
        val engine = StubSessionEngine(registry, bridge)

        val start = engine.startSession("ws_1", "shell")
        engine.writeSession(start.sessionId, Base64.getEncoder().encodeToString("ping".toByteArray()))
        engine.stopSession(start.sessionId, "TERM")

        Thread.sleep(250)

        val events = bridge.eventsFor(start.sessionId)
        val eventTypes = events.mapNotNull { it["type"] as? String }

        assertTrue(eventTypes.contains("state"))
        assertTrue(eventTypes.contains("stdout"))
        assertTrue(eventTypes.last() == "exit")

        val stdoutEvent = events.first { it["type"] == "stdout" }
        val chunk = stdoutEvent["chunkBase64"] as String
        val decoded = String(Base64.getDecoder().decode(chunk), StandardCharsets.UTF_8)
        assertEquals("ping", decoded)
    }

    private class CapturingEventBridge : TermuxAgentEventBridge() {
        private val events = Collections.synchronizedMap(
            mutableMapOf<String, MutableList<Map<String, Any?>>>()
        )

        override fun emit(sessionId: String, payload: Map<String, Any?>) {
            events.getOrPut(sessionId) { mutableListOf() }.add(payload)
        }

        fun eventsFor(sessionId: String): List<Map<String, Any?>> {
            return events[sessionId]?.toList() ?: emptyList()
        }
    }
}
