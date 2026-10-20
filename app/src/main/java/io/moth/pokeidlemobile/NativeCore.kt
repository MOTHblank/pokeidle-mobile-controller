package io.moth.pokeidlemobile

/** Rust library is optional for local GeckoView-only debug builds.
 * CI packages the ARM64 Rust library into the APK.
 */
object NativeCore {
    val loaded: Boolean = try {
        System.loadLibrary("moth_mobile_core")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }
    external fun initializeStorage(path: String): Boolean
    external fun validateBootstrap(payload: String): Boolean
    external fun engineVersion(): String
}
