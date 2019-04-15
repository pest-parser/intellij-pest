#![no_mangle]
#![feature(box_syntax, box_patterns)]

pub mod str4j;

pub extern "C" fn run_vm() {
	println!("Test")
}
