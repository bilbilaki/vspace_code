# Flutter-Termux Master TODO

## 1) Scope & Decisions Lock
### Locked Decisions
- [x] Lock custom package/prefix model for the target Flutter app (no direct `com.termux` prefix reuse).
- [x] Lock federated Flutter plugin architecture (Android host implementation + Dart API surface).
- [x] Lock offline-first runtime model with bundled bootstrap/toolchain artifacts only (no runtime download path).
- [x] Lock multiple named workspaces model.
- [x] Lock default app-internal storage model for workspace/runtime data.
- [x] Lock explicit export flow for moving sources out of app-internal storage.
- [x] Lock phased rollout: `1) backend+file transfer`, `2) LSP+run/build`, `3) custom task view`.

### Out of Scope For Now
- [x] Keep full terminal UI embedding out of phase 1.
- [x] Keep runtime network bootstrap/toolchain download out of scope.
- [x] Keep external storage in-place editing out of scope.
- [x] Keep multi-process distributed backend architecture out of scope.

### Definition of Done
- [ ] Define phase 1 DoD checklist (artifacts, acceptance criteria, failure handling).
- [ ] Define phase 2 DoD checklist (artifacts, acceptance criteria, failure handling).
- [ ] Define phase 3 DoD checklist (artifacts, acceptance criteria, failure handling).

## 2) Source-Grounded Discovery Tasks
- [ ] Re-verify JNI/PTY subprocess behavior in [JNI.java](/home/esil/Documents/workspace/termux-app/terminal-emulator/src/main/java/com/termux/terminal/JNI.java).
- [ ] Re-verify native PTY lifecycle and env handling in [termux.c](/home/esil/Documents/workspace/termux-app/terminal-emulator/src/main/jni/termux.c).
- [ ] Re-verify thread and process lifecycle patterns in [TerminalSession.java](/home/esil/Documents/workspace/termux-app/terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java).
- [ ] Re-verify environment construction in [TermuxShellEnvironment.java](/home/esil/Documents/workspace/termux-app/termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxShellEnvironment.java).
- [ ] Re-verify env validation/serialization helpers in [ShellEnvironmentUtils.java](/home/esil/Documents/workspace/termux-app/termux-shared/src/main/java/com/termux/shared/shell/command/environment/ShellEnvironmentUtils.java).
- [ ] Re-verify prefix/package constraints in [TermuxConstants.java](/home/esil/Documents/workspace/termux-app/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java).
- [ ] Re-verify bootstrap extraction and atomic move strategy in [TermuxInstaller.java](/home/esil/Documents/workspace/termux-app/app/src/main/java/com/termux/app/TermuxInstaller.java).
- [ ] Re-verify ABI bootstrap embedding in [termux-bootstrap-zip.S](/home/esil/Documents/workspace/termux-app/app/src/main/cpp/termux-bootstrap-zip.S).
- [ ] Re-verify app build + NDK constraints in [app/build.gradle](/home/esil/Documents/workspace/termux-app/app/build.gradle).
- [ ] Re-verify emulator module build + ABI constraints in [terminal-emulator/build.gradle](/home/esil/Documents/workspace/termux-app/terminal-emulator/build.gradle).
- [ ] Re-verify file import references in [FileReceiverActivity.java](/home/esil/Documents/workspace/termux-app/app/src/main/java/com/termux/app/api/file/FileReceiverActivity.java).
- [ ] Re-verify file share/open references in [TermuxOpenReceiver.java](/home/esil/Documents/workspace/termux-app/app/src/main/java/com/termux/app/TermuxOpenReceiver.java).
- [ ] Re-verify file copy/move helpers in [FileUtils.java](/home/esil/Documents/workspace/termux-app/termux-shared/src/main/java/com/termux/shared/file/FileUtils.java).

## 3) Foundation TODOs (Tiny-to-Big)
### 3.1 Prefix/Bootstrap
- [x] Define custom package/prefix constants for target Flutter app.
- [x] Define runtime root layout under app internal files directory.
- [ ] Create bootstrap artifact matrix checklist per ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` if needed).
- [ ] Define checksum verification checklist for bundled bootstrap archives.
- [ ] Define staging extraction checklist (extract to staging path before activation).
- [ ] Define atomic rename/swap checklist from staging to active runtime.
- [ ] Define `SYMLINKS.txt` parse and symlink restoration checklist.
- [ ] Define bootstrap corruption recovery checklist (clear staging, keep diagnostics, retry path).
- [ ] Define bootstrap versioning and upgrade compatibility checklist.

### 3.2 Workspace Layout
- [x] Define immutable shared runtime base directory checklist.
- [x] Define per-workspace writable directory checklist (`home`, `project`, `tmp`, metadata).
- [x] Define workspace metadata schema checklist (`id`, `name`, `createdAt`, `lastUsedAt`, `state`, `runtimeVersion`).
- [ ] Define workspace state machine checklist (`creating`, `ready`, `deleting`, `failed`).
- [x] Define workspace naming/slug collision checklist.
- [ ] Define lock/serialization checklist for concurrent create/delete/import/export operations.
- [ ] Define stale workspace cleanup checklist on startup.

### 3.3 Environment Builder
- [ ] Build baseline env map from Android process environment.
- [ ] Inject `HOME` per workspace.
- [ ] Inject `PREFIX` per active runtime root.
- [ ] Inject `PATH` with runtime bin priority.
- [ ] Inject `TMPDIR` with workspace/runtime-scoped temp path.
- [ ] Inject `PWD` with resolved absolute working directory.
- [ ] Define `LD_LIBRARY_PATH` policy checklist (only when required by bundled binaries).
- [ ] Validate env names/values checklist before process spawn.
- [ ] Define optional guarded `.env` debug dump checklist.

### 3.4 Session Runtime
- [x] Add session ID generation checklist.
- [x] Add in-memory `SessionRegistry` checklist.
- [ ] Add persistent session snapshot policy checklist (if resumption required).
- [ ] Add process spawn checklist for `shell` sessions (non-PTY).
- [ ] Add process spawn checklist for `lsp` sessions (non-PTY stdio JSON-RPC).
- [ ] Add optional PTY session checklist (kept separate from LSP transport).
- [ ] Add stdin writer thread checklist.
- [ ] Add stdout reader thread checklist.
- [ ] Add stderr reader thread checklist.
- [ ] Add exit watcher checklist.
- [ ] Add cancellation and kill semantics checklist.
- [ ] Add backpressure/chunking checklist for event streaming.
- [ ] Add per-session log correlation checklist.

## 4) Flutter Plugin Contract TODOs
### 4.1 Dart API Methods
- [x] Define `initializeRuntime()` request/response contract.
- [x] Define `listWorkspaces()` request/response contract.
- [x] Define `createWorkspace({name})` request/response contract.
- [x] Define `deleteWorkspace({workspaceId})` request/response contract.
- [x] Define `importIntoWorkspace({workspaceId, sourceUri, mode})` request/response contract.
- [x] Define `exportWorkspace({workspaceId, destinationUri})` request/response contract.
- [x] Define `startSession({workspaceId, sessionKind, executable, args, cwd, env})` request/response contract.
- [x] Define `writeSession({sessionId, bytes/base64})` request/response contract.
- [x] Define `stopSession({sessionId, signal})` request/response contract.
- [x] Define `subscribeSessionEvents({sessionId})` subscription/unsubscription contract.

### 4.2 Session Kinds
- [x] Define `shell` session semantics and constraints.
- [x] Define `lsp` session semantics and constraints.
- [x] Define `pty-terminal` optional session semantics and constraints.

### 4.3 Event Schema
- [x] Define `stdout` payload schema and chunk semantics.
- [x] Define `stderr` payload schema and chunk semantics.
- [x] Define `exit` payload schema (code/signal/source).
- [x] Define `state` payload schema (created/running/stopping/stopped/failed).
- [x] Define `error` payload schema (code, message, details).
- [x] Define `workspaceProgress` payload schema (phase, percent, message).

### 4.4 Android Core Interfaces
- [ ] Define `WorkspaceStore` responsibilities and method signatures.
- [ ] Define `BootstrapInstaller` responsibilities and method signatures.
- [ ] Define `EnvBuilder` responsibilities and method signatures.
- [ ] Define `SessionHost` responsibilities and method signatures.
- [x] Define `SessionRegistry` responsibilities and method signatures.

### 4.5 Cross-Cutting Contract Tasks
- [x] Define Dart typed request/response models checklist for all methods.
- [x] Define stable field naming/versioning checklist for payload schemas.
- [x] Define error taxonomy checklist (`validation`, `io`, `process`, `workspace`, `bootstrap`, `permission`).
- [x] Define channel lifecycle checklist (subscribe/unsubscribe, plugin detach/reattach).
- [ ] Define app-restart session resume behavior checklist.

## 5) File Transfer TODOs
- [x] Define import modes checklist (`file`, `folder`, merge, overwrite, skip-existing).
- [x] Define copy-in workflow checklist from external URI to internal workspace path.
- [ ] Define content URI permission lifecycle checklist.
- [ ] Define large transfer progress event checklist.
- [x] Define export workflow checklist from internal workspace to external destination.
- [x] Define collision policy checklist for import/export targets.
- [x] Define path traversal protection checklist.
- [x] Define unsafe symlink handling checklist.
- [x] Define import/export rollback behavior checklist on partial failure.

## 6) Milestone TODOs
### Phase 1: Backend + File Transfer
- [ ] Runtime bootstrap install/delete/recreate checklist.
- [x] Workspace CRUD checklist.
- [x] Import/export checklist.
- [x] Session framework checklist (without LSP protocol handling).
- [ ] End-to-end smoke checklist with one command.

### Phase 2: LSP + Run/Build
- [ ] LSP stdio transport checklist (`Content-Length` framing, buffering, partial frame handling).
- [ ] LSP lifecycle checklist (`initialize`, requests, notifications, `shutdown`, `exit`).
- [ ] Command execution checklist for `go run` and `go build`.
- [ ] Generic command execution checklist for other toolchains.
- [ ] Multi-session isolation checklist (policy for one/many LSP per workspace).
- [ ] LSP crash recovery/restart checklist.

### Phase 3: Custom Task View
- [ ] Task model checklist (`name`, `command`, `args`, `cwd`, `envOverrides`).
- [ ] Task execution binding checklist to session APIs.
- [ ] Task log/event rendering checklist.
- [ ] Task persistence checklist per workspace.
- [ ] Task import/export checklist for reusable templates.

## 7) Testing & Acceptance TODOs
- [ ] Offline cold-start workspace creation test checklist.
- [ ] Concurrent workspace operation test checklist.
- [ ] Import folder and open internal copy test checklist.
- [x] Export snapshot test checklist.
- [ ] stdout/stderr ordering test checklist.
- [ ] LSP framing validity test checklist.
- [x] Delete workspace with active sessions test checklist.
- [x] Restart recovery test checklist.
- [ ] ABI mismatch/bootstrap corruption failure test checklist.
- [ ] Low-storage failure and rollback test checklist.

## 8) Release/Operations TODOs
- [ ] Logging/telemetry checklist for bootstrap/session/workspace failures.
- [ ] User-facing diagnostics checklist for common failure modes.
- [ ] Versioned migration checklist for workspace metadata/runtime layout changes.
- [ ] Security checklist for permissions and exposed components.
- [ ] Documentation checklist for plugin integrators.
- [ ] Release verification checklist for ABI artifact completeness and checksums.

## 9) Assumptions / Defaults
- [ ] Confirm guide file to edit is [flutter-termux-agent-guide.md](/home/esil/Documents/workspace/termux-app/flutter-termux-agent-guide.md).
- [ ] Confirm result format remains checkbox-first task tracker with minimal prose.
- [ ] Confirm all tasks start unchecked (`[ ]`) and can later include owner/date metadata.
- [x] Confirm internal storage remains default workspace location.
- [x] Confirm external storage usage remains import/export only.
