package com.vspace.code.termuxagent

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class TermuxAgentMethodHandlerTest {

    @Test
    fun startSession_unknownWorkspace_returnsWorkspaceErrorCode() {
        val fixture = createFixture()
        val result = CapturingResult()

        fixture.handler.onMethodCall(
            MethodCall(
                "startSession",
                mapOf(
                    "workspaceId" to "ws_missing",
                    "sessionKind" to "shell",
                    "executable" to "/bin/echo",
                    "args" to listOf("hello"),
                ),
            ),
            result,
        )

        assertEquals(AgentErrorCode.WORKSPACE.wireValue, result.errorCode)
        assertNotNull(result.errorDetails)
        @Suppress("UNCHECKED_CAST")
        val details = result.errorDetails as Map<String, Any?>
        assertEquals(AgentErrorCode.WORKSPACE.wireValue, details["code"])
    }

    @Test
    fun importIntoWorkspace_contentUri_returnsPermissionErrorCode() {
        val fixture = createFixture()
        val workspace = fixture.workspaceRepository.createWorkspace("demo", "stub-v1")
        val result = CapturingResult()

        fixture.handler.onMethodCall(
            MethodCall(
                "importIntoWorkspace",
                mapOf(
                    "workspaceId" to workspace.id,
                    "sourceUri" to "content://docs/document/1",
                    "mode" to "file",
                ),
            ),
            result,
        )

        assertEquals(AgentErrorCode.PERMISSION.wireValue, result.errorCode)
        assertTrue(result.errorMessage?.contains("content://") == true)
    }

    @Test
    fun deleteWorkspace_withActiveSession_stopsSessionBeforeDelete() {
        val fixture = createFixture()
        val workspace = fixture.workspaceRepository.createWorkspace("demo", "stub-v1")

        val startResult = CapturingResult()
        fixture.handler.onMethodCall(
            MethodCall(
                "startSession",
                mapOf(
                    "workspaceId" to workspace.id,
                    "sessionKind" to "shell",
                    "executable" to "/bin/echo",
                    "args" to listOf("hello"),
                ),
            ),
            startResult,
        )
        @Suppress("UNCHECKED_CAST")
        val startPayload = startResult.successValue as Map<String, Any?>
        val sessionId = startPayload["sessionId"] as String

        val deleteResult = CapturingResult()
        fixture.handler.onMethodCall(
            MethodCall(
                "deleteWorkspace",
                mapOf("workspaceId" to workspace.id),
            ),
            deleteResult,
        )
        @Suppress("UNCHECKED_CAST")
        val deletePayload = deleteResult.successValue as Map<String, Any?>
        assertEquals(true, deletePayload["deleted"])

        val writeResult = CapturingResult()
        fixture.handler.onMethodCall(
            MethodCall(
                "writeSession",
                mapOf("sessionId" to sessionId, "bytesBase64" to "aGVsbG8="),
            ),
            writeResult,
        )
        assertEquals(AgentErrorCode.PROCESS.wireValue, writeResult.errorCode)
    }

    private fun createFixture(): Fixture {
        val filesDir = Files.createTempDirectory("termux_agent_handler_test").toFile()
        val runtimeRepository = RuntimeRepository(filesDir)
        val workspaceRepository = WorkspaceRepository(filesDir)
        val sessionRegistry = SessionRegistry()
        val eventBridge = TermuxAgentEventBridge()
        val sessionEngine = StubSessionEngine(sessionRegistry, eventBridge)
        return Fixture(
            workspaceRepository = workspaceRepository,
            handler = TermuxAgentMethodHandler(
                runtimeRepository = runtimeRepository,
                workspaceRepository = workspaceRepository,
                sessionRegistry = sessionRegistry,
                sessionEngine = sessionEngine,
                eventBridge = eventBridge,
            ),
        )
    }

    private data class Fixture(
        val workspaceRepository: WorkspaceRepository,
        val handler: TermuxAgentMethodHandler,
    )

    private class CapturingResult : MethodChannel.Result {
        var successValue: Any? = null
        var errorCode: String? = null
        var errorMessage: String? = null
        var errorDetails: Any? = null
        var notImplementedCalled: Boolean = false

        override fun success(result: Any?) {
            successValue = result
        }

        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
            this.errorCode = errorCode
            this.errorMessage = errorMessage
            this.errorDetails = errorDetails
        }

        override fun notImplemented() {
            notImplementedCalled = true
        }
    }
}
