#![feature(box_syntax, box_patterns)]

use std::{mem, str};

use pest_meta::parser::{self, Rule};

pub mod str4j;

type JavaStr = *mut u8;

#[no_mangle]
/// For sanity checking.
pub extern "C" fn connectivity_check_add(a: i32, b: i32) -> i32 {
    a + b
}

#[no_mangle]
pub extern "C" fn run_vm(
    pest_code: JavaStr,
    pest_code_len: i32,
    rule_name: JavaStr,
    rule_name_len: i32,
    user_code: JavaStr,
    user_code_len: i32,
) -> JavaStr {
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
    // TODO: don't forget to forget about the returned string
    unimplemented!()
}
