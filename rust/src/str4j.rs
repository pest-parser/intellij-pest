use std::alloc::{self, Layout};
use std::mem;

#[unsafe(no_mangle)]
pub extern "C" fn alloc(size: usize) -> *mut u8 {
    unsafe {
        let layout = Layout::from_size_align(size, mem::align_of::<u8>()).unwrap();
        alloc::alloc(layout)
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn dealloc(ptr: *mut u8, size: usize) {
    unsafe {
        let layout = Layout::from_size_align(size, mem::align_of::<u8>()).unwrap();
        alloc::dealloc(ptr, layout);
    }
}
