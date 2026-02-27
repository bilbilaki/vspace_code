package com.vspace.code.termuxagent

import org.junit.Assert.assertTrue
import org.junit.Test

class MethodContractTest {

    @Test
    fun methodHandler_exposesExpectedMethodNames() {
        val expected = setOf(
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

        assertTrue(TermuxAgentMethodHandler.SUPPORTED_METHODS.containsAll(expected))
    }
}
