import 'dart:convert';
import 'dart:typed_data';

enum WorkspaceState {
  creating('creating'),
  ready('ready'),
  deleting('deleting'),
  failed('failed');

  const WorkspaceState(this.wireValue);

  final String wireValue;

  static WorkspaceState fromWireValue(String value) {
    return WorkspaceState.values.firstWhere(
      (state) => state.wireValue == value,
      orElse: () => WorkspaceState.failed,
    );
  }
}

/// Session kinds supported by the Android host.
///
/// `shell`: non-PTY command execution for command/task workflows.
/// `lsp`: non-PTY stdio JSON-RPC transport for language servers.
/// `ptyTerminal`: optional interactive PTY stream for terminal UI scenarios.
enum SessionKind {
  shell('shell'),
  lsp('lsp'),
  ptyTerminal('pty-terminal');

  const SessionKind(this.wireValue);

  final String wireValue;

  static SessionKind fromWireValue(String value) {
    return SessionKind.values.firstWhere(
      (kind) => kind.wireValue == value,
      orElse: () => SessionKind.shell,
    );
  }
}

enum SessionLifecycleState {
  created('created'),
  running('running'),
  stopping('stopping'),
  stopped('stopped'),
  failed('failed');

  const SessionLifecycleState(this.wireValue);

  final String wireValue;

  static SessionLifecycleState fromWireValue(String value) {
    return SessionLifecycleState.values.firstWhere(
      (state) => state.wireValue == value,
      orElse: () => SessionLifecycleState.failed,
    );
  }
}

enum SessionEventType {
  stdout('stdout'),
  stderr('stderr'),
  exit('exit'),
  state('state'),
  error('error'),
  workspaceProgress('workspaceProgress');

  const SessionEventType(this.wireValue);

  final String wireValue;

  static SessionEventType fromWireValue(String value) {
    return SessionEventType.values.firstWhere(
      (event) => event.wireValue == value,
      orElse: () => SessionEventType.error,
    );
  }
}

enum AgentErrorCode {
  validation('validation'),
  io('io'),
  process('process'),
  workspace('workspace'),
  bootstrap('bootstrap'),
  permission('permission'),
  unknown('unknown');

  const AgentErrorCode(this.wireValue);

  final String wireValue;

  static AgentErrorCode fromWireValue(String value) {
    return AgentErrorCode.values.firstWhere(
      (code) => code.wireValue == value,
      orElse: () => AgentErrorCode.unknown,
    );
  }
}

enum ImportMode {
  file('file'),
  folder('folder'),
  merge('merge'),
  overwrite('overwrite'),
  skipExisting('skip-existing');

  const ImportMode(this.wireValue);

  final String wireValue;

  static ImportMode fromWireValue(String value) {
    return ImportMode.values.firstWhere(
      (mode) => mode.wireValue == value,
      orElse: () => ImportMode.file,
    );
  }
}

enum StopSignal {
  term('TERM'),
  int('INT'),
  kill('KILL');

  const StopSignal(this.wireValue);

  final String wireValue;
}

class WorkspaceDescriptor {
  const WorkspaceDescriptor({
    required this.id,
    required this.name,
    required this.createdAt,
    required this.lastUsedAt,
    required this.state,
    required this.runtimeVersion,
  });

  final String id;
  final String name;
  final DateTime createdAt;
  final DateTime lastUsedAt;
  final WorkspaceState state;
  final String runtimeVersion;

  factory WorkspaceDescriptor.fromMap(Map<Object?, Object?> map) {
    return WorkspaceDescriptor(
      id: _string(map, 'id'),
      name: _string(map, 'name'),
      createdAt: _dateTime(map, 'createdAt'),
      lastUsedAt: _dateTime(map, 'lastUsedAt'),
      state: WorkspaceState.fromWireValue(_string(map, 'state')),
      runtimeVersion: _string(map, 'runtimeVersion'),
    );
  }

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'id': id,
      'name': name,
      'createdAt': createdAt.toIso8601String(),
      'lastUsedAt': lastUsedAt.toIso8601String(),
      'state': state.wireValue,
      'runtimeVersion': runtimeVersion,
    };
  }
}

class InitializeRuntimeRequest {
  const InitializeRuntimeRequest({
    this.forceRecreate = false,
    this.runtimeVersion,
  });

  final bool forceRecreate;
  final String? runtimeVersion;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'forceRecreate': forceRecreate,
      'runtimeVersion': runtimeVersion,
    };
  }
}

class InitializeRuntimeResponse {
  const InitializeRuntimeResponse({
    required this.runtimeVersion,
    required this.runtimeRootPath,
    required this.installedNow,
  });

  final String runtimeVersion;
  final String runtimeRootPath;
  final bool installedNow;

  factory InitializeRuntimeResponse.fromMap(Map<Object?, Object?> map) {
    return InitializeRuntimeResponse(
      runtimeVersion: _string(map, 'runtimeVersion'),
      runtimeRootPath: _string(map, 'runtimeRootPath'),
      installedNow: _bool(map, 'installedNow'),
    );
  }
}

class ListWorkspacesResponse {
  const ListWorkspacesResponse({required this.workspaces});

  final List<WorkspaceDescriptor> workspaces;

  factory ListWorkspacesResponse.fromMap(Map<Object?, Object?> map) {
    return ListWorkspacesResponse(
      workspaces: _list(map, 'workspaces')
          .map((entry) => WorkspaceDescriptor.fromMap(_map(entry)))
          .toList(growable: false),
    );
  }
}

class CreateWorkspaceRequest {
  const CreateWorkspaceRequest({required this.name});

  final String name;

  Map<String, Object?> toMap() {
    return <String, Object?>{'name': name};
  }
}

class CreateWorkspaceResponse {
  const CreateWorkspaceResponse({required this.workspace});

  final WorkspaceDescriptor workspace;

  factory CreateWorkspaceResponse.fromMap(Map<Object?, Object?> map) {
    return CreateWorkspaceResponse(
      workspace: WorkspaceDescriptor.fromMap(_map(map['workspace'])),
    );
  }
}

class DeleteWorkspaceRequest {
  const DeleteWorkspaceRequest({required this.workspaceId});

  final String workspaceId;

  Map<String, Object?> toMap() {
    return <String, Object?>{'workspaceId': workspaceId};
  }
}

class DeleteWorkspaceResponse {
  const DeleteWorkspaceResponse({required this.deleted});

  final bool deleted;

  factory DeleteWorkspaceResponse.fromMap(Map<Object?, Object?> map) {
    return DeleteWorkspaceResponse(deleted: _bool(map, 'deleted'));
  }
}

class ImportIntoWorkspaceRequest {
  const ImportIntoWorkspaceRequest({
    required this.workspaceId,
    required this.sourceUri,
    required this.mode,
  });

  final String workspaceId;
  final String sourceUri;
  final ImportMode mode;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'workspaceId': workspaceId,
      'sourceUri': sourceUri,
      'mode': mode.wireValue,
    };
  }
}

class ImportIntoWorkspaceResponse {
  const ImportIntoWorkspaceResponse({required this.importedPath});

  final String importedPath;

  factory ImportIntoWorkspaceResponse.fromMap(Map<Object?, Object?> map) {
    return ImportIntoWorkspaceResponse(
      importedPath: _string(map, 'importedPath'),
    );
  }
}

class ExportWorkspaceRequest {
  const ExportWorkspaceRequest({
    required this.workspaceId,
    required this.destinationUri,
  });

  final String workspaceId;
  final String destinationUri;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'workspaceId': workspaceId,
      'destinationUri': destinationUri,
    };
  }
}

class ExportWorkspaceResponse {
  const ExportWorkspaceResponse({required this.exportedUri});

  final String exportedUri;

  factory ExportWorkspaceResponse.fromMap(Map<Object?, Object?> map) {
    return ExportWorkspaceResponse(exportedUri: _string(map, 'exportedUri'));
  }
}

class StartSessionRequest {
  const StartSessionRequest({
    required this.workspaceId,
    required this.sessionKind,
    required this.executable,
    this.args = const <String>[],
    this.cwd,
    this.env,
  });

  final String workspaceId;
  final SessionKind sessionKind;
  final String executable;
  final List<String> args;
  final String? cwd;
  final Map<String, String>? env;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'workspaceId': workspaceId,
      'sessionKind': sessionKind.wireValue,
      'executable': executable,
      'args': args,
      'cwd': cwd,
      'env': env,
    };
  }
}

class StartSessionResponse {
  const StartSessionResponse({required this.sessionId, required this.state});

  final String sessionId;
  final SessionLifecycleState state;

  factory StartSessionResponse.fromMap(Map<Object?, Object?> map) {
    return StartSessionResponse(
      sessionId: _string(map, 'sessionId'),
      state: SessionLifecycleState.fromWireValue(_string(map, 'state')),
    );
  }
}

class WriteSessionRequest {
  const WriteSessionRequest({
    required this.sessionId,
    required this.bytesBase64,
  });

  factory WriteSessionRequest.fromBytes({
    required String sessionId,
    required Uint8List bytes,
  }) {
    return WriteSessionRequest(
      sessionId: sessionId,
      bytesBase64: base64Encode(bytes),
    );
  }

  final String sessionId;
  final String bytesBase64;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'sessionId': sessionId,
      'bytesBase64': bytesBase64,
    };
  }
}

class WriteSessionResponse {
  const WriteSessionResponse({required this.acceptedBytes});

  final int acceptedBytes;

  factory WriteSessionResponse.fromMap(Map<Object?, Object?> map) {
    return WriteSessionResponse(acceptedBytes: _int(map, 'acceptedBytes'));
  }
}

class StopSessionRequest {
  const StopSessionRequest({
    required this.sessionId,
    this.signal = StopSignal.term,
  });

  final String sessionId;
  final StopSignal signal;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'sessionId': sessionId,
      'signal': signal.wireValue,
    };
  }
}

class StopSessionResponse {
  const StopSessionResponse({required this.stopped});

  final bool stopped;

  factory StopSessionResponse.fromMap(Map<Object?, Object?> map) {
    return StopSessionResponse(stopped: _bool(map, 'stopped'));
  }
}

class SubscribeSessionEventsRequest {
  const SubscribeSessionEventsRequest({required this.sessionId});

  final String sessionId;

  Map<String, Object?> toMap() {
    return <String, Object?>{'sessionId': sessionId};
  }
}

class SessionExitPayload {
  const SessionExitPayload({required this.code, this.signal, this.source});

  final int code;
  final String? signal;
  final String? source;

  factory SessionExitPayload.fromMap(Map<Object?, Object?> map) {
    return SessionExitPayload(
      code: _int(map, 'code'),
      signal: _nullableString(map, 'signal'),
      source: _nullableString(map, 'source'),
    );
  }

  Map<String, Object?> toMap() {
    return <String, Object?>{'code': code, 'signal': signal, 'source': source};
  }
}

class SessionErrorPayload {
  const SessionErrorPayload({
    required this.code,
    required this.message,
    this.details,
  });

  final AgentErrorCode code;
  final String message;
  final Object? details;

  factory SessionErrorPayload.fromMap(Map<Object?, Object?> map) {
    return SessionErrorPayload(
      code: AgentErrorCode.fromWireValue(_string(map, 'code')),
      message: _string(map, 'message'),
      details: map['details'],
    );
  }

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'code': code.wireValue,
      'message': message,
      'details': details,
    };
  }
}

class WorkspaceProgressPayload {
  const WorkspaceProgressPayload({
    required this.phase,
    required this.percent,
    required this.message,
  });

  final String phase;
  final double percent;
  final String message;

  factory WorkspaceProgressPayload.fromMap(Map<Object?, Object?> map) {
    return WorkspaceProgressPayload(
      phase: _string(map, 'phase'),
      percent: _double(map, 'percent'),
      message: _string(map, 'message'),
    );
  }

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'phase': phase,
      'percent': percent,
      'message': message,
    };
  }
}

/// Event payload envelope sent through the session event channel.
///
/// The `type` field determines which optional payload fields are set.
class SessionEvent {
  const SessionEvent({
    required this.sessionId,
    required this.type,
    required this.timestamp,
    this.chunkBase64,
    this.chunkIndex,
    this.state,
    this.exit,
    this.error,
    this.workspaceProgress,
  });

  final String sessionId;
  final SessionEventType type;
  final DateTime timestamp;
  final String? chunkBase64;
  final int? chunkIndex;
  final SessionLifecycleState? state;
  final SessionExitPayload? exit;
  final SessionErrorPayload? error;
  final WorkspaceProgressPayload? workspaceProgress;

  factory SessionEvent.fromMap(Map<Object?, Object?> map) {
    return SessionEvent(
      sessionId: _string(map, 'sessionId'),
      type: SessionEventType.fromWireValue(_string(map, 'type')),
      timestamp: _dateTime(map, 'timestamp'),
      chunkBase64: _nullableString(map, 'chunkBase64'),
      chunkIndex: _nullableInt(map, 'chunkIndex'),
      state: map['state'] == null
          ? null
          : SessionLifecycleState.fromWireValue(map['state'] as String),
      exit: map['exit'] == null
          ? null
          : SessionExitPayload.fromMap(_map(map['exit'])),
      error: map['error'] == null
          ? null
          : SessionErrorPayload.fromMap(_map(map['error'])),
      workspaceProgress: map['workspaceProgress'] == null
          ? null
          : WorkspaceProgressPayload.fromMap(_map(map['workspaceProgress'])),
    );
  }

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'sessionId': sessionId,
      'type': type.wireValue,
      'timestamp': timestamp.toIso8601String(),
      'chunkBase64': chunkBase64,
      'chunkIndex': chunkIndex,
      'state': state?.wireValue,
      'exit': exit?.toMap(),
      'error': error?.toMap(),
      'workspaceProgress': workspaceProgress?.toMap(),
    };
  }

  Uint8List? decodeChunk() {
    if (chunkBase64 == null) {
      return null;
    }
    return base64Decode(chunkBase64!);
  }
}

class TermuxAgentException implements Exception {
  const TermuxAgentException({
    required this.code,
    required this.message,
    this.details,
  });

  final AgentErrorCode code;
  final String message;
  final Object? details;

  factory TermuxAgentException.fromMap(Map<Object?, Object?> map) {
    return TermuxAgentException(
      code: AgentErrorCode.fromWireValue(_string(map, 'code')),
      message: _string(map, 'message'),
      details: map['details'],
    );
  }

  @override
  String toString() {
    return 'TermuxAgentException(code: ${code.wireValue}, message: $message, details: $details)';
  }
}

Map<Object?, Object?> _map(Object? value) {
  if (value is Map<Object?, Object?>) {
    return value;
  }
  if (value is Map) {
    return Map<Object?, Object?>.from(value);
  }
  throw ArgumentError('Expected map but got ${value.runtimeType}');
}

List<Object?> _list(Map<Object?, Object?> map, String key) {
  final Object? value = map[key];
  if (value is List<Object?>) {
    return value;
  }
  if (value is List) {
    return List<Object?>.from(value);
  }
  throw ArgumentError('Expected list for "$key" but got ${value.runtimeType}');
}

String _string(Map<Object?, Object?> map, String key) {
  final Object? value = map[key];
  if (value is String) {
    return value;
  }
  throw ArgumentError(
    'Expected string for "$key" but got ${value.runtimeType}',
  );
}

String? _nullableString(Map<Object?, Object?> map, String key) {
  final Object? value = map[key];
  if (value == null) {
    return null;
  }
  if (value is String) {
    return value;
  }
  throw ArgumentError(
    'Expected nullable string for "$key" but got ${value.runtimeType}',
  );
}

bool _bool(Map<Object?, Object?> map, String key) {
  final Object? value = map[key];
  if (value is bool) {
    return value;
  }
  throw ArgumentError('Expected bool for "$key" but got ${value.runtimeType}');
}

int _int(Map<Object?, Object?> map, String key) {
  final Object? value = map[key];
  if (value is int) {
    return value;
  }
  throw ArgumentError('Expected int for "$key" but got ${value.runtimeType}');
}

int? _nullableInt(Map<Object?, Object?> map, String key) {
  final Object? value = map[key];
  if (value == null) {
    return null;
  }
  if (value is int) {
    return value;
  }
  throw ArgumentError(
    'Expected nullable int for "$key" but got ${value.runtimeType}',
  );
}

double _double(Map<Object?, Object?> map, String key) {
  final Object? value = map[key];
  if (value is double) {
    return value;
  }
  if (value is int) {
    return value.toDouble();
  }
  throw ArgumentError(
    'Expected double for "$key" but got ${value.runtimeType}',
  );
}

DateTime _dateTime(Map<Object?, Object?> map, String key) {
  final String value = _string(map, key);
  return DateTime.parse(value).toUtc();
}
