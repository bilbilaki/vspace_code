import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:vspace_code/flutter_termux_agent/flutter_termux_agent.dart';

void main() {
  group('models', () {
    test('workspace descriptor roundtrip', () {
      final descriptor = WorkspaceDescriptor(
        id: 'ws_1_abcd',
        name: 'demo',
        createdAt: DateTime.parse('2026-01-01T00:00:00.000Z'),
        lastUsedAt: DateTime.parse('2026-01-01T00:01:00.000Z'),
        state: WorkspaceState.ready,
        runtimeVersion: 'stub-v1',
      );

      final decoded = WorkspaceDescriptor.fromMap(descriptor.toMap());
      expect(decoded.id, descriptor.id);
      expect(decoded.state, WorkspaceState.ready);
      expect(decoded.runtimeVersion, 'stub-v1');
    });

    test('session event decode chunk', () {
      final bytes = Uint8List.fromList(utf8.encode('abc'));
      final event = SessionEvent.fromMap(<Object?, Object?>{
        'sessionId': 'ss_1_abcd',
        'type': 'stdout',
        'timestamp': '2026-01-01T00:00:00.000Z',
        'chunkBase64': base64Encode(bytes),
        'chunkIndex': 1,
      });

      expect(event.type, SessionEventType.stdout);
      expect(event.decodeChunk(), bytes);
      expect(event.chunkIndex, 1);
    });

    test('error code mapping fallback', () {
      expect(
        AgentErrorCode.fromWireValue('workspace'),
        AgentErrorCode.workspace,
      );
      expect(AgentErrorCode.fromWireValue('not-real'), AgentErrorCode.unknown);
    });

    test('request map includes required fields', () {
      final request = StartSessionRequest(
        workspaceId: 'ws_1_abcd',
        sessionKind: SessionKind.shell,
        executable: '/bin/echo',
        args: const <String>['hello'],
      );

      final map = request.toMap();
      expect(map['workspaceId'], 'ws_1_abcd');
      expect(map['sessionKind'], 'shell');
      expect(map['executable'], '/bin/echo');
      expect(map['args'], const <String>['hello']);
    });
  });
}
