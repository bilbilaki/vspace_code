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

    private var methodChannel: MethodChannel? = null
    private var eventChannel: EventChannel? = null
    private var eventBridge: TermuxAgentEventBridge? = null
    private var sessionEngine: StubSessionEngine? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val runtimeRepository = RuntimeRepository(filesDir)
        val workspaceRepository = WorkspaceRepository(filesDir)
        val sessionRegistry = SessionRegistry()
        val bridge = TermuxAgentEventBridge()
        val stubSessionEngine = StubSessionEngine(sessionRegistry, bridge)
        val handler = TermuxAgentMethodHandler(
            runtimeRepository = runtimeRepository,
            workspaceRepository = workspaceRepository,
            sessionRegistry = sessionRegistry,
            sessionEngine = stubSessionEngine,
            eventBridge = bridge,
        )

        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL).also {
            it.setMethodCallHandler(handler)
        }
        eventChannel = EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL).also {
            it.setStreamHandler(bridge)
        }
        eventBridge = bridge
        sessionEngine = stubSessionEngine
    }

    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        methodChannel?.setMethodCallHandler(null)
        eventChannel?.setStreamHandler(null)
        eventBridge?.clearAll()
        sessionEngine?.shutdown()
        methodChannel = null
        eventChannel = null
        eventBridge = null
        sessionEngine = null
        super.cleanUpFlutterEngine(flutterEngine)
    }
}
