# GeckoView authentication feasibility experiment

The **first APK is a controlled experiment**, not a release of native Moth automation.

## Current flow

1. A single GeckoRuntime embeds GeckoView in a native Android Activity.
2. Four account buttons create distinct persistent `GeckoSessionSettings.contextId`
   storage partitions (`moth-game-1` through `moth-game-4`).
3. The user opens PokéIdle and signs in inside GeckoView. Twitch is reachable
   through the Twitch button within the same account context.
4. OAuth popups are represented by new GeckoSessions inheriting the same
   account context; Back returns to the account's main session.
5. A built-in, restricted WebExtension runs at document_start only on
   https://pokeidle.io and its subdomains. Native messaging is accepted only
   for top-level sender frames from those HTTPS origins.
6. The experiment checks whether GeckoView's WebExtension can:
   - attach to PokéIdle pages;
   - install a WebSocket.prototype.send observer;
   - notice a **game hello frame** without transferring the frame;
   - access the browser cookies API without transferring names or values.
7. The result is displayed as flags on the app itself. `cookie_api_available`
   establishes API availability, NOT a valid authenticated PokéIdle session.

## Manual checks on a real ARM64 device

- Install the debug APK. Verify GeckoView renders the PokéIdle login screen.
- Sign in via the game's supported flow. Record whether Google or Twitch rejects
  the embedded browser, and whether an OAuth popup opens and returns correctly.
- Observe `content_attached`, `page_dom_ready`,
  `websocket_observer_installed`, and `game_hello_observed`.
- Switching to Game 2 must not show Game 1 as already signed in.
- Switching back to Game 1 should retain its account cookies.
- If `websocket_observer_unavailable` appears, investigate GeckoView's
  Firefox-specific Xray/exportFunction behavior. It is NOT a successful handoff.
- On phones with the Rust JNI binary packaged, the status shows the Rust
  engine version. Without it, the GeckoView-only experiment still runs.

## Strict boundaries

This experiment never copies an authentication secret from the Gecko process.
The WebSocket observer sends only an event **name**, never the hello frame.
Cookie reads return only API capability; no cookie material crosses JNI or
native messaging. No keys, credentials or captured frames belong in logs or
screenshots. Do not treat a page event as confirmation from the game server.

Future work must implement a secure per-profile authentication transfer,
validate the hello and Twitch IRC handshake, protect credentials using the
Android Keystore, and confirm both native authenticated connections before
closing a GeckoView session. At that point use the imported
`NativeSession::start_verified`, not an unverified imitation.
Foreground-service restrictions, reconnects, and network changes also need
a device-tested Android lifecycle implementation.

The Google OAuth path may reject embedded browsers. GeckoView is used only as
a feasibility experiment; we do not bypass the identity provider's rules.
