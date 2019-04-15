#![feature(box_syntax, box_patterns)]

pub mod str4j;

type JavaStr = *mut u8;

#[no_mangle]
/// For sanity checking.
pub extern "C" fn connectivity_check_add(a: u32, b: u32) -> u32 {
	a + b
}

#[no_mangle]
pub extern "C" fn run_vm(pest_code: JavaStr, rule_name: JavaStr, ) {
	println!("Test")
}
