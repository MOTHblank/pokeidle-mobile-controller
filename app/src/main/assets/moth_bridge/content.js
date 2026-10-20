"use strict";
// Runs ONLY on PokéIdle pages. This proof-of-concept sends no cookies, tokens,
// WebSocket payloads, player names, or other personal information to Android.
(() => {
  const notify = (kind, info = {}) => {
    browser.runtime.sendNativeMessage("moth", { kind, ...info }).catch(() => {});
  };
  notify("content_attached");
  // Firefox-only Xray-unwrapping API. Do not overwrite a page's WebSocket
  // constructor; watch just the prototype send method for handshake metadata.
  try {
    const prototype = window.wrappedJSObject.WebSocket.prototype;
    const originalSend = prototype.send;
    let foundHello = false;
    prototype.send = exportFunction(function (payload) {
      if (!foundHello && typeof payload === "string" && payload.length < 32768) {
        try {
          if (JSON.parse(payload).t === "hello") {
            foundHello = true;
            // Intentionally discard the actual game hello and its contents.
            notify("game_hello_observed");
          }
        } catch (_ignored) { /* Non-JSON, not an authenticated hello. */ }
      }
      return originalSend.call(this, payload);
    }, prototype);
    notify("websocket_observer_installed");
  } catch (_error) {
    notify("websocket_observer_unavailable");
  }

  // The background extension may read *only* PokéIdle's cookie metadata.
  // Do not store or forward cookie names or values in this experiment.
  browser.runtime.sendMessage({ kind: "cookie_capability" }).then(
    (result) => notify(result && result.available ? "cookie_api_available" : "cookie_api_unavailable")
  ).catch(() => notify("cookie_api_unavailable"));

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => notify("page_dom_ready"), { once: true });
  } else {
    notify("page_dom_ready");
  }
})();
