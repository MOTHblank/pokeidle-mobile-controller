//! Android-only storage and four game profiles. No desktop Firefox paths.
use std::path::PathBuf;
use std::sync::OnceLock;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum GameProfile { Game1, Game2, Game3, Game4 }
impl GameProfile {
    pub const ALL: [Self; 4] = [Self::Game1, Self::Game2, Self::Game3, Self::Game4];
    pub const fn index(self) -> usize {
        match self { Self::Game1 => 0, Self::Game2 => 1, Self::Game3 => 2, Self::Game4 => 3 }
    }
    pub const fn from_index(index: usize) -> Option<Self> {
        match index { 0 => Some(Self::Game1), 1 => Some(Self::Game2),
            2 => Some(Self::Game3), 3 => Some(Self::Game4), _ => None }
    }
    pub const fn name(self) -> &'static str {
        match self { Self::Game1 => "Game1", Self::Game2 => "Game2",
            Self::Game3 => "Game3", Self::Game4 => "Game4" }
    }
}
static ROOT: OnceLock<PathBuf> = OnceLock::new();

pub fn initialize_data_root(path: PathBuf) -> Result<(), String> {
    if !path.is_absolute() { return Err("Private app storage path must be absolute".into()); }
    std::fs::create_dir_all(&path).map_err(|_| "Cannot initialize app-private storage".to_owned())?;
    if let Some(prior) = ROOT.get() {
        return if *prior == path { Ok(()) } else { Err("Storage already initialized".into()) };
    }
    ROOT.set(path).map_err(|_| "Storage already initialized".to_owned())
}
pub(crate) fn data_root() -> Result<PathBuf, String> {
    if let Some(root) = ROOT.get() { return Ok(root.clone()); }
    #[cfg(test)]
    { return Ok(std::env::temp_dir().join("moth-mobile-core-tests")); }
    #[allow(unreachable_code)]
    Err("App-private storage has not been initialized".into())
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn four_slots_are_distinct() {
        for (i, p) in GameProfile::ALL.into_iter().enumerate() {
            assert_eq!(GameProfile::from_index(i), Some(p));
            assert_eq!(p.index(), i);
        }
        assert_eq!(GameProfile::from_index(4), None);
    }
}
