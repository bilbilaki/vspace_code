package com.vspace.code

import com.vspace.code.termuxagent.RuntimeRepository
import com.vspace.code.termuxagent.SessionRegistry
import com.vspace.code.termuxagent.StubSessionEngine
import com.vspace.code.termuxagent.TermuxAgentEventBridge
import com.vspace.code.termuxagent.TermuxAgentMethodHandler
import com.vspace.code.termuxagent.WorkspaceRepository
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        private const val METHOD_CHANNEL = "vspace.termux_agent/methods"
        private const val EVENT_CHANNEL = "vspace.termux_agent/session_events"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val runtimeRepository = RuntimeRepository(filesDir)
        val workspaceRepository = WorkspaceRepository(filesDir)
        val sessionRegistry = SessionRegistry()
        val eventBridge = TermuxAgentEventBridge()
        val stubSessionEngine = StubSessionEngine(sessionRegistry, eventBridge)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL)
            .setMethodCallHandler(
                TermuxAgentMethodHandler(
                    runtimeRepository = runtimeRepository,
                    workspaceRepository = workspaceRepository,
                    sessionRegistry = sessionRegistry,
                    sessionEngine = stubSessionEngine,
                    eventBridge = eventBridge,
                )
            )

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
            .setStreamHandler(eventBridge)
    }
}
