package io.moth.pokeidlemobile

import java.net.URI

/** Never accept arbitrary page-supplied event types or untrusted origins.
 * Events are informational hints only, never proof of authentication.
 */
object BridgeEventPolicy {
    private val kinds = setOf(
        "content_attached", "page_dom_ready",
        "websocket_observer_installed", "websocket_observer_unavailable",
        "game_hello_observed", "cookie_api_available", "cookie_api_unavailable"
    )
    fun allow(kind: String, senderUrl: String, isTopLevel: Boolean): Boolean {
        if (!isTopLevel || kind !in kinds) return false
        val uri = try { URI(senderUrl) } catch (_: Exception) { return false }
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme == "https" && (host == "pokeidle.io" || host.endsWith(".pokeidle.io"))
    }
}
