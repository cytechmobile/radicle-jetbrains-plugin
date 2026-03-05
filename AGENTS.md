# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run plugin in development IDE instance
./gradlew runIde

# Run unit/integration tests (excludes UI tests)
./gradlew test

# Run a specific test class
./gradlew test --tests "network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindowTest"

# Run UI tests (requires runIdeForUiTests running)
./gradlew uiTest

# Run end-to-end tests
./gradlew endToEndTests

# Run IDE with Remote Robot for UI testing
./gradlew runIdeForUiTests
```

## How to run the UI tests

UI tests require a virtual framebuffer (Xvfb) and the IDE running with Remote Robot before the test task is invoked. The Makefile handles all of this automatically.

### All-in-one (recommended)

```bash
make ui-test-run
```

This starts Xvfb if needed, launches the IDE in the background, waits for the Remote Robot server on port 8082, runs the UI tests, then shuts the IDE down.

### Step by step (e.g. when developing tests interactively)

```bash
# Terminal 1 — start IDE, leave it running
make ui-ide

# Terminal 2 — once the IDE is ready, run tests
make ui-test-run   # or: JAVA_HOME=... ./gradlew :uiTest
```

### Unit/integration tests

```bash
make test
```

### How the Makefile handles environment issues

- **`JAVA_HOME`** is always auto-detected via `readlink -f /usr/bin/java` so the stale amd64 path that the Vagrantfile writes to `.bashrc` on arm64 VMs is ignored. Override on the command line if needed: `make JAVA_HOME=/custom/path test`.
- **`DISPLAY`** defaults to `:99` and is exported to all child processes.
- **`xvfb-start`** (a dependency of `test`, `ui-ide`, and `ui-test-run`) starts Xvfb on `$(DISPLAY)` if it is not already running.

## Code Quality

- Checkstyle enforced with zero tolerance (0 errors, 0 warnings)
- Run `./gradlew checkstyleMain checkstyleTest` to check style

## Architecture

This is a JetBrains IntelliJ plugin for [Radicle](https://radicle.xyz), a decentralized code collaboration protocol. The plugin integrates with the `rad` CLI tool and provides UI for patches (similar to PRs) and issues.

### Key Packages

- **actions/rad/**: Commands wrapping `rad` CLI operations. `RadAction` is the base class - subclasses implement `run()` and `getActionName()`. Commands requiring identity unlock override `shouldUnlockIdentity()`.
- **services/**: Core services registered in plugin.xml
  - `RadicleProjectService`: Executes `rad` CLI commands, handles Git operations
  - `RadicleCliService`: Higher-level operations (patches, issues, comments)
  - `RadicleNativeService`: JNR-FFI bridge to native Rust library (`jrad/`) for operations not available via CLI
- **toolwindow/**: Main UI - `RadicleToolWindow` hosts Patches and Issues tabs
- **patches/**: Patch listing, creation, review UI, inline code comments
- **issues/**: Issue listing, creation, overview UI
- **models/**: Data classes (`RadPatch`, `RadIssue`, `RadProject`, `RadAuthor`, etc.)
- **config/**: Settings stored via `RadicleProjectSettingsHandler`

### Native Integration (jrad/)

The `jrad/` directory contains a Rust library providing operations not available via CLI. It's loaded via JNR-FFI on macOS/Linux or executed as a binary via WSL on Windows. The `JRad` interface defines available native methods.

### Testing

Tests extend `AbstractIT` which provides:
- Git repository setup with rad remote configured
- `RadStub` for mocking CLI command outputs
- `RadicleNativeStub` for mocking native service
- Notification queue capture for assertions
- `executeUiTasks()` for flushing UI event queue in tests

UI tests use JetBrains Remote Robot framework with `@Video` annotation for recording.

### Plugin Configuration

- `gradle.properties`: Plugin metadata, IntelliJ platform version (2025.3), Java 21
- `plugin.xml`: Extension points, services, actions, tool window registration
- Plugin depends on: Git4Idea, Markdown plugin

### Windows Support

Radicle CLI runs in WSL. The plugin handles path translation and executes commands via `wsl bash -ic`.


## Self-maintenance

After making changes that affect project conventions, structure, or business logic documented here, update this file to reflect those changes.

## Ground rules

- Tell me things I need to know even if I don't want to hear it
- Push back when something seems wrong - don't just agree with mistakes
- Ask me questions when something is unclear and you need to make a choice. Don't just choose randomly if it's important for what we're doing.
- When you show me a potential error or miss, start your response with ❗️emoji.
- **ALWAYS** start replies with STARTER_CHARACTER + space (default: 🤖)
- Verify all tests are passing after making changes to the codebase.