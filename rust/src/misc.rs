use std::mem;

/// Represents a Java string.
pub type JavaStr = *mut u8;

#[unsafe(no_mangle)]
/// For sanity checking.
pub extern "C" fn connectivity_check_add(a: i32, b: i32) -> i32 {
    a + b
}

#[unsafe(no_mangle)]
pub extern "C" fn crate_info() -> JavaStr {
    let version = env!("CARGO_PKG_VERSION");
    let authors = env!("CARGO_PKG_AUTHORS");
    let descrip = env!("CARGO_PKG_DESCRIPTION");
    let s = format!("{}\n{}\n{}", version, authors, descrip);
    let p = s.as_ptr();
    mem::forget(s);
    p as _
}
