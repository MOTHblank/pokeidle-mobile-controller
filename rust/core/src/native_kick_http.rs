//! Android-safe replacement for the desktop shell-out-to-curl adapter.
//! Never execute system commands from Android and never follow redirects.
use serde_json::Value;
use reqwest::blocking::Client;
use reqwest::redirect::Policy;
use std::time::Duration;

pub fn get(url: &str, bearer: Option<&str>, extra_headers: &[(&str, String)])
    -> Result<(u16, Value), String>
{
    let parsed = reqwest::Url::parse(url).map_err(|_| "Invalid Kick endpoint")?;
    let allowed = parsed.scheme() == "https" &&
        ((parsed.host_str() == Some("kick.com") &&
            parsed.path().starts_with("/api/v2/channels/")) ||
         (parsed.host_str() == Some("websockets.kick.com") &&
            parsed.path() == "/viewer/v1/token"));
    if !allowed || parsed.username() != "" || parsed.password().is_some() {
        return Err("Kick request blocked: unexpected URL".into());
    }
    let client = Client::builder().redirect(Policy::none())
        .timeout(Duration::from_secs(12)).build()
        .map_err(|_| "Kick HTTP setup failed")?;
    let mut request = client.get(parsed)
        .header("Accept", "application/json, text/plain, */*")
        .header("Origin", "https://kick.com")
        .header("Referer", "https://kick.com/");
    if let Some(token) = bearer {
        if token.len() > 8192 || token.bytes().any(|b| b.is_ascii_control()) {
            return Err("Invalid Kick authorization".into());
        }
        request = request.bearer_auth(token);
    }
    for (name, value) in extra_headers {
        if !matches!(*name, "X-Chatroom" | "X-User-Id" | "Content-Type" | "X-Client-Token")
            || value.len() > 8192 || value.bytes().any(|b| b.is_ascii_control())
        {
            return Err("Unexpected Kick HTTP header".into());
        }
        request = request.header(*name, value.as_str());
    }
    let response = request.send().map_err(|_| "Kick request failed")?;
    let status = response.status().as_u16();
    if response.content_length().is_some_and(|len| len > 1_048_576) {
        return Err("Kick response too large".into());
    }
    let bytes = response.bytes().map_err(|_| "Could not read Kick response")?;
    if bytes.len() > 1_048_576 { return Err("Kick response too large".into()); }
    let value = serde_json::from_slice::<Value>(&bytes).unwrap_or(Value::Null);
    Ok((status, value))
}
