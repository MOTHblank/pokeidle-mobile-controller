//! The prototype deliberately suppresses native logs. No browser-auth
//! material may reach Android logcat, panic payloads, or crash telemetry.
#[allow(dead_code)]
pub fn info(_message: &str) {}
#[allow(dead_code)]
pub fn warn(_message: &str) {}
#[allow(dead_code)]
pub fn error(_message: &str) {}
