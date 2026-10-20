//! Native engine source reused from Moth Controller.
//! The GeckoView experiment reports *only non-secret readiness events*;
//! native handoff must not start until secure credentials are available.
#![allow(dead_code)]
mod config;
mod logging;
mod native_tls;
mod native_capture_config;
mod native_atlas_models;
mod native_items;
mod native_gameplay;
mod native_hunts;
mod native_automation;
mod native_restock;
mod native_history;
mod native_market;
mod native_twitch;
mod native_kick_hls;
mod native_kick_http;
mod native_kick_viewer;
mod native_kick;
mod native_atlas;
mod native_background;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jstring};
use serde_json::Value;
use std::path::PathBuf;

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_moth_pokeidlemobile_NativeCore_initializeStorage(
    mut env: JNIEnv, _class: JClass, path: JString
) -> jboolean {
    let Ok(path) = env.get_string(&path) else { return 0 };
    let root = PathBuf::from(path.to_string_lossy().as_ref());
    (config::initialize_data_root(root).is_ok() && native_tls::ensure().is_ok()) as jboolean
}

/// Does NOT persist, log or transmit the bootstrap.
#[unsafe(no_mangle)]
pub extern "system" fn Java_io_moth_pokeidlemobile_NativeCore_validateBootstrap(
    mut env: JNIEnv, _class: JClass, payload: JString
) -> jboolean {
    let Ok(payload) = env.get_string(&payload) else { return 0 };
    let valid = serde_json::from_str::<Value>(&payload.to_string_lossy()).ok()
        .is_some_and(|value| native_background::validate_bootstrap(&value).is_ok());
    valid as jboolean
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_moth_pokeidlemobile_NativeCore_engineVersion(
    env: JNIEnv, _class: JClass
) -> jstring {
    env.new_string(env!("CARGO_PKG_VERSION"))
        .map(|value| value.into_raw())
        .unwrap_or(std::ptr::null_mut())
}
