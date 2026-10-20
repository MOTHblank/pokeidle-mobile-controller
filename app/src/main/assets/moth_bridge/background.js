"use strict";
// Test cookie-access capability WITHOUT forwarding names or values.
browser.runtime.onMessage.addListener(async (message) => {
  if (!message || message.kind !== "cookie_capability") return undefined;
  try {
    const cookies = await browser.cookies.getAll({ url: "https://pokeidle.io/app" });
    return { available: true, anyCookies: cookies.length > 0 };
  } catch (_error) {
    return { available: false, anyCookies: false };
  }
});
