import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';

import 'flutter_termux_agent/flutter_termux_agent.dart';

void main() {
  runApp(const MainApp());
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Termux Agent Demo',
      home: const AgentDemoPage(),
      theme: ThemeData(useMaterial3: true),
    );
  }
}

class AgentDemoPage extends StatefulWidget {
  const AgentDemoPage({super.key});

  @override
  State<AgentDemoPage> createState() => _AgentDemoPageState();
}

class _AgentDemoPageState extends State<AgentDemoPage> {
  final FlutterTermuxAgentClient _client = FlutterTermuxAgentClient();

  StreamSubscription<SessionEvent>? _sessionSubscription;
  String? _activeWorkspaceId;
  String? _activeSessionId;
  List<WorkspaceDescriptor> _workspaces = <WorkspaceDescriptor>[];
  final StringBuffer _logs = StringBuffer();

  @override
  void dispose() {
    _sessionSubscription?.cancel();
    super.dispose();
  }

  Future<void> _initializeRuntime() async {
    try {
      final InitializeRuntimeResponse response = await _client
          .initializeRuntime(const InitializeRuntimeRequest());
      _appendLog(
        'initializeRuntime => version=${response.runtimeVersion}, '
        'path=${response.runtimeRootPath}, installedNow=${response.installedNow}',
      );
    } catch (error) {
      _appendLog('initializeRuntime error: $error');
    }
  }

  Future<void> _createWorkspace() async {
    try {
      final CreateWorkspaceResponse response = await _client.createWorkspace(
        CreateWorkspaceRequest(
          name: 'demo-${DateTime.now().millisecondsSinceEpoch}',
        ),
      );
      _activeWorkspaceId = response.workspace.id;
      _appendLog('createWorkspace => id=${response.workspace.id}');
      await _listWorkspaces();
    } catch (error) {
      _appendLog('createWorkspace error: $error');
    }
  }

  Future<void> _listWorkspaces() async {
    try {
      final ListWorkspacesResponse response = await _client.listWorkspaces();
      setState(() {
        _workspaces = response.workspaces;
        _activeWorkspaceId =
            _activeWorkspaceId ??
            (_workspaces.isNotEmpty ? _workspaces.first.id : null);
      });
      _appendLog('listWorkspaces => count=${response.workspaces.length}');
    } catch (error) {
      _appendLog('listWorkspaces error: $error');
    }
  }

  Future<void> _startSession() async {
    final String? workspaceId = _activeWorkspaceId;
    if (workspaceId == null) {
      _appendLog('startSession skipped: no active workspace');
      return;
    }

    try {
      final StartSessionResponse response = await _client.startSession(
        StartSessionRequest(
          workspaceId: workspaceId,
          sessionKind: SessionKind.shell,
          executable: '/bin/echo',
          args: const <String>['hello'],
        ),
      );

      _activeSessionId = response.sessionId;
      _appendLog(
        'startSession => sessionId=${response.sessionId} state=${response.state.wireValue}',
      );

      await _sessionSubscription?.cancel();
      _sessionSubscription = _client
          .subscribeSessionEvents(
            SubscribeSessionEventsRequest(sessionId: response.sessionId),
          )
          .listen(
            (SessionEvent event) {
              _appendLog(
                'event(${event.type.wireValue}) => ${jsonEncode(event.toMap())}',
              );
            },
            onError: (Object error) {
              _appendLog('event stream error: $error');
            },
          );
    } catch (error) {
      _appendLog('startSession error: $error');
    }
  }

  Future<void> _writeSession() async {
    final String? sessionId = _activeSessionId;
    if (sessionId == null) {
      _appendLog('writeSession skipped: no active session');
      return;
    }

    try {
      final payload = utf8.encode('hello-from-dart');
      final WriteSessionResponse response = await _client.writeSession(
        WriteSessionRequest.fromBytes(sessionId: sessionId, bytes: payload),
      );
      _appendLog('writeSession => acceptedBytes=${response.acceptedBytes}');
    } catch (error) {
      _appendLog('writeSession error: $error');
    }
  }

  Future<void> _stopSession() async {
    final String? sessionId = _activeSessionId;
    if (sessionId == null) {
      _appendLog('stopSession skipped: no active session');
      return;
    }

    try {
      final StopSessionResponse response = await _client.stopSession(
        StopSessionRequest(sessionId: sessionId),
      );
      _appendLog('stopSession => stopped=${response.stopped}');
      await _client.unsubscribeSessionEvents(sessionId);
      await _sessionSubscription?.cancel();
      _sessionSubscription = null;
      _activeSessionId = null;
    } catch (error) {
      _appendLog('stopSession error: $error');
    }
  }

  void _appendLog(String value) {
    setState(() {
      _logs.writeln('[${DateTime.now().toIso8601String()}] $value');
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Flutter Termux Agent')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: <Widget>[
                ElevatedButton(
                  onPressed: _initializeRuntime,
                  child: const Text('Initialize Runtime'),
                ),
                ElevatedButton(
                  onPressed: _createWorkspace,
                  child: const Text('Create Workspace'),
                ),
                ElevatedButton(
                  onPressed: _listWorkspaces,
                  child: const Text('List Workspaces'),
                ),
                ElevatedButton(
                  onPressed: _startSession,
                  child: const Text('Start Session'),
                ),
                ElevatedButton(
                  onPressed: _writeSession,
                  child: const Text('Write Session'),
                ),
                ElevatedButton(
                  onPressed: _stopSession,
                  child: const Text('Stop Session'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text('Active workspace: ${_activeWorkspaceId ?? 'none'}'),
            Text('Active session: ${_activeSessionId ?? 'none'}'),
            const SizedBox(height: 12),
            Text('Workspaces (${_workspaces.length}):'),
            for (final WorkspaceDescriptor workspace in _workspaces)
              Text(
                '- ${workspace.id} (${workspace.name}) [${workspace.state.wireValue}]',
              ),
            const SizedBox(height: 12),
            const Text('Logs:'),
            const SizedBox(height: 8),
            Expanded(
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey.shade400),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SingleChildScrollView(
                  child: SelectableText(_logs.toString()),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
