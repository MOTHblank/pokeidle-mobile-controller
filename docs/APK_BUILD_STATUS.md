# APK build status

The initial GitHub Actions runs failed before Android code compiled, because
`android-actions/setup-android@v3` attempted to install the retired Android SDK
`tools` package. The workflow now uses `setup-android@v4` with
`packages: platform-tools`.

The prototype targets **ARM64 Android**. Its dependency is the official
`geckoview-arm64-v8a` artifact, not the larger multi-architecture `geckoview`
artifact. The APK workflow now validates that both the built-in WebExtension
manifest and its content script are packaged. A missing APK or extension causes
a workflow failure.

The Rust cross-build remains intentionally best-effort while JNI is under
development. When it fails, CI emits a warning and clearly marks the artifact
as **GeckoView-only**. This does not mean native game sessions work; the first
device milestone is still the GeckoView login and handshake-readiness probe.

A build passing in CI is not equivalent to a successful Google/Twitch login
or a verified PokéIdle WebSocket on a real phone.
