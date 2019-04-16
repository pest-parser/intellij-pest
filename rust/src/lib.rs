#![feature(box_syntax, box_patterns)]

use std::alloc::System;
use std::ffi::CString;
use std::{mem, str};

use pest::error::{Error, ErrorVariant};
use pest_meta::parser::{self, Rule};
use pest_meta::{optimizer, validator};
use pest_vm::Vm;

pub mod str4j;

type JavaStr = *mut u8;

#[global_allocator]
static GLOBAL_ALLOCATOR: System = System;
static mut VM: Option<Vm> = None;

#[no_mangle]
/// For sanity checking.
pub extern "C" fn connectivity_check_add(a: i32, b: i32) -> i32 {
    a + b
}

#[inline]
fn convert_error(error: Error<Rule>) -> String {
    match error.variant {
        ErrorVariant::CustomError { message } => message,
        _ => unreachable!(),
    }
}

#[no_mangle]
pub extern "C" fn load_vm(pest_code: JavaStr, pest_code_len: i32) -> JavaStr {
    let pest_code_len = pest_code_len as usize;
    let pest_code_bytes =
        unsafe { Vec::<u8>::from_raw_parts(pest_code, pest_code_len, pest_code_len) };
    let pest_code = str::from_utf8(&pest_code_bytes).unwrap();
    let pest_code_result = parser::parse(Rule::grammar_rules, pest_code).map_err(|error| {
        error.renamed_rules(|rule| match *rule {
            Rule::grammar_rule => "rule".to_owned(),
            Rule::_push => "push".to_owned(),
            Rule::assignment_operator => "`=`".to_owned(),
            Rule::silent_modifier => "`_`".to_owned(),
            Rule::atomic_modifier => "`@`".to_owned(),
            Rule::compound_atomic_modifier => "`$`".to_owned(),
            Rule::non_atomic_modifier => "`!`".to_owned(),
            Rule::opening_brace => "`{`".to_owned(),
            Rule::closing_brace => "`}`".to_owned(),
            Rule::opening_paren => "`(`".to_owned(),
            Rule::positive_predicate_operator => "`&`".to_owned(),
            Rule::negative_predicate_operator => "`!`".to_owned(),
            Rule::sequence_operator => "`&`".to_owned(),
            Rule::choice_operator => "`|`".to_owned(),
            Rule::optional_operator => "`?`".to_owned(),
            Rule::repeat_operator => "`*`".to_owned(),
            Rule::repeat_once_operator => "`+`".to_owned(),
            Rule::comma => "`,`".to_owned(),
            Rule::closing_paren => "`)`".to_owned(),
            Rule::quote => "`\"`".to_owned(),
            Rule::insensitive_string => "`^`".to_owned(),
            Rule::range_operator => "`..`".to_owned(),
            Rule::single_quote => "`'`".to_owned(),
            other_rule => format!("{:?}", other_rule),
        })
    });
    let pairs = match pest_code_result {
        Ok(pairs) => pairs,
        Err(error) => {
            let cstr = CString::new(format!("Err[{:?}]", convert_error(error))).unwrap();
            let ptr = cstr.as_ptr() as *mut _;
            mem::forget(cstr);
            return ptr;
        }
    };

    if let Err(errors) = validator::validate_pairs(pairs.clone()) {
        let cstr = CString::new(format!(
            "Err{:?}",
            errors.into_iter().map(convert_error).collect::<Vec<_>>()
        ))
        .unwrap();
        let ptr = cstr.as_ptr() as *mut _;
        mem::forget(cstr);
        return ptr;
    }

    let ast = match parser::consume_rules(pairs) {
        Ok(ast) => ast,
        Err(errors) => {
            let cstr = CString::new(format!(
                "Err{:?}",
                errors.into_iter().map(convert_error).collect::<Vec<_>>()
            ))
            .unwrap();
            let ptr = cstr.as_ptr() as *mut _;
            mem::forget(cstr);
            return ptr;
        }
    };

    let rules: Vec<_> = ast.iter().map(|rule| rule.name.clone()).collect();
    unsafe {
        VM = Some(Vm::new(optimizer::optimize(ast)));
    }

    let cstr = CString::new(format!("{:?}", rules)).unwrap();
    let ptr = cstr.as_ptr() as *mut _;
    mem::forget(pest_code_bytes);
    mem::forget(cstr);
    ptr
}

#[no_mangle]
pub extern "C" fn render_code(
    rule_name: JavaStr,
    rule_name_len: i32,
    user_code: JavaStr,
    user_code_len: i32,
) -> JavaStr {
    // TODO: don't forget to forget about the returned string
    unimplemented!()
}
