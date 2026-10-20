# Moth Mobile Controller

Experimental **Android/ARM64 GeckoView and native Rust** port of
[Moth Controller](https://github.com/MOTHblank/pokeidle-addons).

The initial objective is to establish whether a GeckoView-based app can
authenticate PokéIdle and observe the session bootstrap in a controlled way
before attempting the browser-to-native handoff.

## What is implemented

- Native Android application written in Kotlin (minimum Android 8, API 26).
- GeckoView 158.0.20261007201254 (pinned stable), with a built-in WebExtension.
- Four account selectors with independent persistent GeckoView session contexts.
- PokéIdle and Twitch navigation, with OAuth popup/session support.
- A metadata-only probe for WebSocket hello observation and browser cookie
  capability. **No cookies, hello frames or credentials are forwarded.**
- 16 reusable Rust engine source modules copied from desktop Moth Controller:
  game transport, Auto Catch, restock, market/Watch, Twitch, Kick, Hunt Atlas
  models, battle history and related helpers.
- Android-specific Rust configuration and JNI bootstrap validation endpoints.
- GitHub Actions debug APK workflow. A Rust ARM64 build is attempted; a
  GeckoView-only APK is produced if the Rust build is not available.

## Build

Open this repository in **Android Studio** with JDK 17, Android SDK 35 and
Gradle 8.13. Mozilla's Maven repository is included in settings.gradle.kts.

If you have Gradle 8.13 installed on PATH:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

The Gradle wrapper properties are pinned, but the binary wrapper JAR is not
committed in this initial bootstrap. You can generate a local wrapper with
`gradle wrapper --gradle-version 8.13` if you want `./gradlew`.

To build the JNI engine on a Linux development machine with Android NDK:

```bash
rustup target add aarch64-linux-android
export ANDROID_NDK_HOME="${ANDROID_HOME}/ndk/27.0.12077973"
export PATH="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=aarch64-linux-android26-clang
export CC_aarch64_linux_android=aarch64-linux-android26-clang
export AR_aarch64_linux_android=llvm-ar
cargo build --manifest-path rust/core/Cargo.toml --release --target aarch64-linux-android
mkdir -p app/src/main/jniLibs/arm64-v8a
cp rust/core/target/aarch64-linux-android/release/libmoth_mobile_core.so app/src/main/jniLibs/arm64-v8a/
gradle :app:assembleDebug
```

## Test status and limitations

The APK is **not yet a working mobile game automation client**. The first
experiment does not perform native session handoff, store credentials, automate
purchases, or run a foreground service. A metadata-only hello observation
is not proof of authenticated gameplay. See
[the test plan](docs/GECKOVIEW_EXPERIMENT.md) and
[the Rust import notes](docs/RUST_IMPORT.md).

Sign-in providers may reject embedded browsers. Do not bypass provider security
restrictions. Android background execution and four-account native operation
must be validated separately before any production release.

Licensed under MIT.
