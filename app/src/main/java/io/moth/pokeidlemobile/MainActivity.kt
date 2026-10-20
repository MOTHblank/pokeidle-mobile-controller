package io.moth.pokeidlemobile

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension

/**
 * One visible GeckoView, four isolated persistent session contexts.
 * Browser authentication is experimental: there is no native handoff yet.
 * No credentials, cookies or handshake payloads cross the bridge.
 */
class MainActivity : Activity() {
    companion object {
        private const val GAME = "https://pokeidle.io/app"
        private const val TWITCH = "https://www.twitch.tv/"
        private const val BRIDGE_PATH = "resource://android/assets/moth_bridge/"
        private const val BRIDGE_ID = "moth-mobile-bridge@pokeidle.local"
        private const val NATIVE_APP = "moth"
        private var sharedRuntime: GeckoRuntime? = null
    }

    private lateinit var runtime: GeckoRuntime
    private lateinit var browser: GeckoView
    private lateinit var status: TextView
    private var extension: WebExtension? = null
    private val mainSessions = arrayOfNulls<GeckoSession>(4)
    private val ownedSessions = mutableMapOf<GeckoSession, Int>()
    private val events = Array(4) { linkedSetOf<String>() }
    private val canGoBack = mutableMapOf<GeckoSession, Boolean>()
    private var displayedSession: GeckoSession? = null
    private var slot = 0
    private var extensionState = "Starting"
    private var coreState = "Not packaged"

    private val messages = object : WebExtension.MessageDelegate {
        override fun onMessage(
            nativeApp: String,
            message: Any,
            sender: WebExtension.MessageSender
        ): GeckoResult<Any>? {
            if (nativeApp != NATIVE_APP || message !is JSONObject) return null
            val kind = message.optString("kind")
            if (!BridgeEventPolicy.allow(kind, sender.url, sender.isTopLevel)) return null
            val owner = ownedSessions[sender.session] ?: return null
            // Only allow the 7 known non-secret readiness flags through.
            events[owner].add(kind)
            renderStatus()
            return null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        coreState = if (NativeCore.loaded) {
            val root = java.io.File(filesDir, "moth-native").absolutePath
            if (NativeCore.initializeStorage(root)) {
                "Rust " + NativeCore.engineVersion()
            } else {
                "Rust storage/TLS initialization failed"
            }
        } else "Rust library not packaged in this build"
        runtime = sharedRuntime ?: GeckoRuntime.create(applicationContext).also { sharedRuntime = it }
        renderStatus()
        runtime.webExtensionController.ensureBuiltIn(BRIDGE_PATH, BRIDGE_ID).accept(
            { installed ->
                extension = installed
                extensionState = "Built-in extension installed"
                for (session in ownedSessions.keys) {
                    session.webExtensionController.setMessageDelegate(installed, messages, NATIVE_APP)
                }
                selectSlot(0)
            },
            { _ ->
                extensionState = "Extension installation failed (browser still usable)"
                selectSlot(0)
            }
        )
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "Moth Mobile — GeckoView experiment"
            textSize = 18f
            setPadding(16, 12, 16, 8)
        }
        root.addView(title)

        val scroll = HorizontalScrollView(this)
        val accounts = LinearLayout(this)
        repeat(4) { index ->
            accounts.addView(Button(this).apply {
                text = "Game " + (index + 1)
                setOnClickListener { selectSlot(index) }
            })
        }
        scroll.addView(accounts)
        root.addView(scroll)

        val actions = LinearLayout(this)
        fun action(name: String, run: () -> Unit) {
            actions.addView(Button(this).apply {
                text = name
                setOnClickListener { run() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        action("PokéIdle") { displayedSession?.loadUri(GAME) }
        action("Twitch") { displayedSession?.loadUri(TWITCH) }
        action("Reload") { displayedSession?.reload() }
        action("Back") { goBack() }
        root.addView(actions)

        status = TextView(this).apply {
            textSize = 12f
            setPadding(16, 8, 16, 8)
        }
        root.addView(status)
        browser = GeckoView(this)
        root.addView(browser, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun createSession(owner: Int, popup: Boolean): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .contextId("moth-game-" + (owner + 1))
            .build()
        val session = GeckoSession(settings)
        ownedSessions[session] = owner
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onCloseRequest(closing: GeckoSession) {
                if (closing !== mainSessions[owner]) {
                    if (displayedSession === closing) showSession(mainSessions[owner]!!)
                    ownedSessions.remove(closing)
                    closing.close()
                }
            }
        }
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(current: GeckoSession, canGo: Boolean) {
                canGoBack[current] = canGo
            }
            override fun onNewSession(source: GeckoSession, uri: String): GeckoResult<GeckoSession> {
                // Do not open the new session here; GeckoView opens it after return.
                val child = createSession(owner, true)
                browser.post { showSession(child) }
                return GeckoResult.fromValue(child)
            }
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStop(current: GeckoSession, success: Boolean) {
                if (ownedSessions[current] == slot) renderStatus()
            }
        }
        extension?.let { session.webExtensionController.setMessageDelegate(it, messages, NATIVE_APP) }
        if (!popup) session.open(runtime)
        return session
    }

    private fun showSession(session: GeckoSession) {
        if (displayedSession === session) return
        browser.releaseSession()
        browser.setSession(session)
        displayedSession = session
        renderStatus()
    }

    private fun selectSlot(index: Int) {
        slot = index
        val session = mainSessions[index] ?: createSession(index, false).also {
            mainSessions[index] = it
            it.loadUri(GAME)
        }
        showSession(session)
    }

    private fun goBack() {
        val showing = displayedSession ?: return
        val root = mainSessions[slot]
        if (showing !== root && root != null) {
            showSession(root)
            return
        }
        if (canGoBack[showing] == true) showing.goBack()
    }

    private fun renderStatus() {
        if (!::status.isInitialized) return
        val flags = events[slot].joinToString(", ").ifEmpty { "No page signals yet" }
        status.text = "Account " + (slot + 1) + " (isolated)\n" +
            "GeckoView: " + extensionState + "\n" +
            "Native: " + coreState + "\n" +
            "Signals: " + flags + "\n" +
            "Do not regard page signals as a verified game connection."
    }

    override fun onBackPressed() {
        if (displayedSession !== mainSessions[slot] ||
            canGoBack[displayedSession] == true) goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        browser.releaseSession()
        for (session in ownedSessions.keys.toList()) session.close()
        ownedSessions.clear()
        super.onDestroy()
    }
}
