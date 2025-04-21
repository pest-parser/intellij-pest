/*!
# Pest IDE

This is a bridge library for the [IntelliJ IDEA plugin for Pest][jb].

 [jb]: https://plugins.jetbrains.com/plugin/12046-pest
 [asmble]:https://github.com/cretz/asmble

It's supposed to be compiled only with the wasm32 backend of nightly rustc
(at least at this moment).

After compiling as wasm, it's translated to JVM bytecode with [asmble][asmble] and then
loaded in the plugin.
Thus no JNI.
*/

#![feature(box_patterns)]
#[rustc_box]

use std::alloc::System;
use std::ffi::CString;
use std::{mem, str};

use pest::error::{Error, ErrorVariant, InputLocation};
use pest::iterators::Pair;
use pest_meta::parser::{self, Rule};
use pest_meta::{optimizer, validator};
use pest_vm::Vm;

use self::misc::JavaStr;

/// Allocation library for Java use.
///
/// On Java side, it can create a Rust string based on the codes in this module.
pub mod str4j;

/// Everything that are not related to pest.
pub mod misc;

#[global_allocator]
static GLOBAL_ALLOCATOR: System = System;
static mut VM: Option<Vm> = None;

/// From position to line-column pair.
fn line_col(pos: usize, input: &str) -> (usize, usize) {
    let mut pos = pos;
    // Position's pos is always a UTF-8 border.
    let slice = &input[..pos];
    let mut chars = slice.chars().peekable();

    let mut line_col = (1, 1);

    while pos != 0 {
        match chars.next() {
            Some('\r') => {
                if let Some(&'\n') = chars.peek() {
                    chars.next();

                    if pos == 1 {
                        pos -= 1;
                    } else {
                        pos -= 2;
                    }

                    line_col = (line_col.0 + 1, 1);
                } else {
                    pos -= 1;
                    line_col = (line_col.0, line_col.1 + 1);
                }
            }
            Some('\n') => {
                pos -= 1;
                line_col = (line_col.0 + 1, 1);
            }
            Some(c) => {
                pos -= c.len_utf8();
                line_col = (line_col.0, line_col.1 + 1);
            }
            None => unreachable!(),
        }
    }

    line_col
}

/// Convert the error to a readable format.
fn convert_error(error: Error<Rule>, grammar: &str) -> String {
    let message = match error.variant {
        ErrorVariant::CustomError { message } => message,
        _ => unreachable!(),
    };
    let ((start_line, start_col), (end_line, end_col)) = match error.location {
        InputLocation::Pos(pos) => (line_col(pos, grammar), line_col(pos, grammar)),
        InputLocation::Span((start, end)) => (line_col(start, grammar), line_col(end, grammar)),
    };
    format!(
        "{:?}^{:?}^{:?}^{:?}^{}",
        start_line, start_col, end_line, end_col, message
    )
}

#[unsafe(no_mangle)]
/// Load the Pest VM as a global variable.
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
        Err(err) => {
            let cstr = CString::new(format!("Err[{:?}]", convert_error(err, &pest_code))).unwrap();
            let ptr = cstr.as_ptr() as *mut _;
            mem::forget(pest_code_bytes);
            mem::forget(cstr);
            return ptr;
        }
    };

    if let Err(errors) = validator::validate_pairs(pairs.clone()) {
        let cstr = CString::new(format!(
            "Err{:?}",
            errors
                .into_iter()
                .map(|e| convert_error(e, &pest_code))
                .collect::<Vec<_>>()
        ))
        .unwrap();
        let ptr = cstr.as_ptr() as *mut _;
        mem::forget(pest_code_bytes);
        mem::forget(cstr);
        return ptr;
    }

    let ast = match parser::consume_rules(pairs) {
        Ok(ast) => ast,
        Err(errors) => {
            let cstr = CString::new(format!(
                "Err{:?}",
                errors
                    .into_iter()
                    .map(|e| convert_error(e, &pest_code))
                    .collect::<Vec<_>>()
            ))
            .unwrap();
            let ptr = cstr.as_ptr() as *mut _;
            mem::forget(pest_code_bytes);
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

/// Convert the pair information to a string that the plugin
/// understands.
fn join_pairs(result: &mut Vec<String>, pair: Pair<&str>) {
    let span = pair.as_span();
    let start = span.start();
    let end = span.end();
    result.push(format!("{:?}^{:?}^{}", start, end, pair.as_rule()));
    for child in pair.into_inner() {
        join_pairs(result, child);
    }
}

#[unsafe(no_mangle)]
/// After loading the VM, this function can parse the code with the
/// currently loaded VM.
/// Assumes the VM is already loaded, otherwise it'll panic.
pub extern "C" fn render_code(
    rule_name: JavaStr,
    rule_name_len: i32,
    user_code: JavaStr,
    user_code_len: i32,
) -> JavaStr {
    let vm = unsafe { VM.as_ref().unwrap() };
    let rule_name_len = rule_name_len as usize;
    let rule_name_bytes =
        unsafe { Vec::<u8>::from_raw_parts(rule_name, rule_name_len, rule_name_len) };
    let rule_name = str::from_utf8(&rule_name_bytes).unwrap();
    let user_code_len = user_code_len as usize;
    let user_code_bytes =
        unsafe { Vec::<u8>::from_raw_parts(user_code, user_code_len, user_code_len) };
    let user_code = str::from_utf8(&user_code_bytes).unwrap();
    let cstr = CString::new(match vm.parse(rule_name, user_code) {
        Ok(pairs) => {
            let mut result = vec![];
            for pair in pairs {
                join_pairs(&mut result, pair);
            }
            format!("{:?}", result)
        }
        Err(err) => format!("Err{}", err.renamed_rules(|r| r.to_string())),
    })
    .unwrap();
    mem::forget(rule_name_bytes);
    mem::forget(user_code_bytes);
    let ptr = cstr.as_ptr() as *mut _;
    mem::forget(cstr);
    ptr
}
