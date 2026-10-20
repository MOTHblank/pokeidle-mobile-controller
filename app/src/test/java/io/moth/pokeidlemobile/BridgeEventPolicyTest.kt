package io.moth.pokeidlemobile

import org.junit.Assert.*
import org.junit.Test

class BridgeEventPolicyTest {
    @Test fun onlyKnownFirstPartyTopLevelEventsAreAccepted() {
        assertTrue(BridgeEventPolicy.allow("game_hello_observed", "https://pokeidle.io/app", true))
        assertTrue(BridgeEventPolicy.allow("page_dom_ready", "https://foo.pokeidle.io/a", true))
        assertFalse(BridgeEventPolicy.allow("game_hello_observed", "https://pokeidle.io.attacker.test/app", true))
        assertFalse(BridgeEventPolicy.allow("game_hello_observed", "http://pokeidle.io/app", true))
        assertFalse(BridgeEventPolicy.allow("game_hello_observed", "https://pokeidle.io/app", false))
        assertFalse(BridgeEventPolicy.allow("cookies", "https://pokeidle.io/app", true))
    }
}
