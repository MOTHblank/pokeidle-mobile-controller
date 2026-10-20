# Reusable engine code import

Source: https://github.com/MOTHblank/pokeidle-addons
Branch: `master`; source checked on 2026-10-19.
License: MIT, preserved in LICENSE.

The 16 `rust/core/src/native_*.rs` gameplay, market, restock, Twitch, Kick,
Hunt Atlas, and history files were copied directly from the desktop controller
into this repository using GitHub blob content.

Desktop-only modules `app.rs`, `firefox.rs`, `monitor.rs`,
`native_auth.rs`, `native_kick_login.rs`, and the desktop updater are not
part of the Android engine.

Android adapters added:
- `config.rs`: four GameProfile slots and an Android app-private data root.
- `native_kick_http.rs`: reqwest HTTPS adapter replacing system curl.
- `logging.rs`: no-op logging to prevent authentication telemetry leaks during
  the prototype.
- `lib.rs`: JNI initialization, engine version and bootstrap validation. It
  does NOT start authenticated sessions or persist credentials.

The original Rust code has a large number of interdependent modules. This is
an import and build-integration milestone, **not** proof that those systems have
been verified on Android. Native core builds are currently best-effort in CI,
and the APK can run in a GeckoView-only mode.

Do not store bootstrap data in SharedPreferences, files, console logs,
WebExtension page events, or Android intents. Future encrypted storage must
use Android Keystore-backed authenticated encryption.
