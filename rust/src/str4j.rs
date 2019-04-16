use std::alloc::{self, Layout};
use std::ffi::CString;
use std::os::raw::c_char;
use std::{mem, str};

#[no_mangle]
pub extern "C" fn string_len(ptr: *mut u8, len: usize) -> usize {
    let bytes = unsafe { Vec::<u8>::from_raw_parts(ptr, len, len) };
    let len = str::from_utf8(&bytes).unwrap().chars().count();
    mem::forget(bytes);
    len
}

#[no_mangle]
pub extern "C" fn prepend_from_rust(ptr: *mut u8, len: usize) -> *const c_char {
    let bytes = unsafe { Vec::<u8>::from_raw_parts(ptr, len, len) };
    let cstr = CString::new(format!("From Rust: {}", str::from_utf8(&bytes).unwrap())).unwrap();
    println!("{:?}", cstr);
    let ret = cstr.as_ptr();
    mem::forget(bytes);
    mem::forget(cstr);
    ret
}

#[no_mangle]
pub extern "C" fn alloc(size: usize) -> *mut u8 {
    unsafe {
        let layout = Layout::from_size_align(size, mem::align_of::<u8>()).unwrap();
        alloc::alloc(layout)
    }
}

#[no_mangle]
pub extern "C" fn dealloc(ptr: *mut u8, size: usize) {
    unsafe {
        let layout = Layout::from_size_align(size, mem::align_of::<u8>()).unwrap();
        alloc::dealloc(ptr, layout);
    }
}
