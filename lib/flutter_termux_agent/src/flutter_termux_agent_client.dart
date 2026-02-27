import 'dart:async';

import 'package:flutter/services.dart';

import 'models.dart';

class FlutterTermuxAgentClient {
  FlutterTermuxAgentClient({
    MethodChannel? methodChannel,
    EventChannel? eventChannel,
  }) : _methodChannel =
           methodChannel ?? const MethodChannel(_defaultMethodChannelName),
       _eventChannel =
           eventChannel ?? const EventChannel(_defaultEventChannelName);

  static const String _defaultMethodChannelName = 'vspace.termux_agent/methods';
  static const String _defaultEventChannelName =
      'vspace.termux_agent/session_events';

  final MethodChannel _methodChannel;
  final EventChannel _eventChannel;
  final Map<String, Stream<SessionEvent>> _sessionEventStreams =
      <String, Stream<SessionEvent>>{};

  Future<InitializeRuntimeResponse> initializeRuntime(
    InitializeRuntimeRequest request,
  ) async {
    final Map<Object?, Object?> result = await _invokeMap(
      'initializeRuntime',
      request.toMap(),
    );
    return InitializeRuntimeResponse.fromMap(result);
  }

  Future<ListWorkspacesResponse> listWorkspaces() async {
    final Map<Object?, Object?> result = await _invokeMap('listWorkspaces');
    return ListWorkspacesResponse.fromMap(result);
  }

  Future<CreateWorkspaceResponse> createWorkspace(
    CreateWorkspaceRequest request,
  ) async {
    final Map<Object?, Object?> result = await _invokeMap(
      'createWorkspace',
      request.toMap(),
    );
    return CreateWorkspaceResponse.fromMap(result);
  }

  Future<DeleteWorkspaceResponse> deleteWorkspace(
    DeleteWorkspaceRequest request,
  ) async {
    final Map<Object?, Object?> result = await _invokeMap(
      'deleteWorkspace',
      request.toMap(),
    );
    return DeleteWorkspaceResponse.fromMap(result);
  }

  Future<ImportIntoWorkspaceResponse> importIntoWorkspace(
    ImportIntoWorkspaceRequest request,
  ) async {
    final Map<Object?, Object?> result = await _invokeMap(
      'importIntoWorkspace',
      request.toMap(),
    );
    return ImportIntoWorkspaceResponse.fromMap(result);
  }

  Future<ExportWorkspaceResponse> exportWorkspace(
    ExportWorkspaceRequest request,
  ) async {
    final Map<Object?, Object?> result = await _invokeMap(
      'exportWorkspace',
      request.toMap(),
    );
    return ExportWorkspaceResponse.fromMap(result);
  }

  Future<StartSessionResponse> startSession(StartSessionRequest request) async {
    final Map<Object?, Object?> result = await _invokeMap(
      'startSession',
      request.toMap(),
    );
    return StartSessionResponse.fromMap(result);
  }

  Future<WriteSessionResponse> writeSession(WriteSessionRequest request) async {
    final Map<Object?, Object?> result = await _invokeMap(
      'writeSession',
      request.toMap(),
    );
    return WriteSessionResponse.fromMap(result);
  }

  Future<StopSessionResponse> stopSession(StopSessionRequest request) async {
    final Map<Object?, Object?> result = await _invokeMap(
      'stopSession',
      request.toMap(),
    );
    return StopSessionResponse.fromMap(result);
  }

  Future<void> unsubscribeSessionEvents(String sessionId) async {
    _sessionEventStreams.remove(sessionId);
    await _invokeVoid('unsubscribeSessionEvents', <String, Object?>{
      'sessionId': sessionId,
    });
  }

  Stream<SessionEvent> subscribeSessionEvents(
    SubscribeSessionEventsRequest request,
  ) {
    final String sessionId = request.sessionId;
    final Stream<SessionEvent>? existing = _sessionEventStreams[sessionId];
    if (existing != null) {
      return existing;
    }

    final Stream<SessionEvent> stream = _eventChannel
        .receiveBroadcastStream(request.toMap())
        .map<Map<Object?, Object?>>((dynamic event) {
          if (event is Map<Object?, Object?>) {
            return event;
          }
          if (event is Map) {
            return Map<Object?, Object?>.from(event);
          }
          throw const FormatException('Session event payload must be a map.');
        })
        .map<SessionEvent>(SessionEvent.fromMap)
        .asBroadcastStream(
          onCancel: (StreamSubscription<SessionEvent> subscription) {
            subscription.cancel();
          },
        );

    _sessionEventStreams[sessionId] = stream;
    return stream;
  }

  Future<Map<Object?, Object?>> _invokeMap(
    String method, [
    Map<String, Object?>? arguments,
  ]) async {
    try {
      final Object? result = await _methodChannel.invokeMethod<Object?>(
        method,
        arguments,
      );
      if (result is Map<Object?, Object?>) {
        return result;
      }
      if (result is Map) {
        return Map<Object?, Object?>.from(result);
      }
      throw FormatException('Method "$method" returned ${result.runtimeType}.');
    } on PlatformException catch (error) {
      throw _platformExceptionToDomain(error);
    }
  }

  Future<void> _invokeVoid(
    String method, [
    Map<String, Object?>? arguments,
  ]) async {
    try {
      await _methodChannel.invokeMethod<void>(method, arguments);
    } on PlatformException catch (error) {
      throw _platformExceptionToDomain(error);
    }
  }

  TermuxAgentException _platformExceptionToDomain(PlatformException exception) {
    final AgentErrorCode code = AgentErrorCode.fromWireValue(exception.code);

    if (exception.details is Map<Object?, Object?>) {
      final Map<Object?, Object?> details =
          exception.details as Map<Object?, Object?>;
      return TermuxAgentException.fromMap(<Object?, Object?>{
        'code': details['code'] ?? code.wireValue,
        'message': details['message'] ?? (exception.message ?? 'Unknown error'),
        'details': details['details'],
      });
    }

    if (exception.details is Map) {
      final Map<Object?, Object?> details = Map<Object?, Object?>.from(
        exception.details as Map,
      );
      return TermuxAgentException.fromMap(<Object?, Object?>{
        'code': details['code'] ?? code.wireValue,
        'message': details['message'] ?? (exception.message ?? 'Unknown error'),
        'details': details['details'],
      });
    }

    return TermuxAgentException(
      code: code,
      message: exception.message ?? 'Unknown platform error',
      details: exception.details,
    );
  }
}
