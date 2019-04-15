#![no_mangle]
#![feature(box_syntax, box_patterns)]

pub mod str4j;

type JavaStr = *mut u8;

pub extern "C" fn run_vm(pest_code: JavaStr, rule_name: JavaStr) {
	println!("Test")
}
