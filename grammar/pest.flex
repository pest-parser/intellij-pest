package rs.pest.psi;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static rs.pest.psi.PestTokenType.*;
import static rs.pest.psi.PestTypes.*;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;

%%

%{
  public PestLexer() { this((java.io.Reader)null); }

  private int commentStart = 0;
  private int commentDepth = 0;
%}

%public
%class PestLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode
%ignorecase
%eof{ return;
%eof}

%state INSIDE_COMMENT

WHITE_SPACE=[\ \t\f\r\n]
INTEGER=[0-9]+
IDENTIFIER=[a-zA-Z_][a-zA-Z_0-9]*
STRING_UNICODE=\\((u\{{HEXDIGIT}{2,6}\})|(x{HEXDIGIT}{2}))
STRING_INCOMPLETE=\"([^\"\\]|(\\[^])|{STRING_UNICODE})*
CHAR_INCOMPLETE='([^\"\\]|(\\[^])|{STRING_UNICODE})?
STRING_LITERAL={STRING_INCOMPLETE}\"
CHAR_LITERAL={CHAR_INCOMPLETE}'
HEXDIGIT=[a-fA-F0-9]

%%

<INSIDE_COMMENT> {
	"/*" { ++commentDepth; }
	"*/" { if (--commentDepth <= 0) {yybegin(YYINITIAL); zzStartRead = commentStart; return BLOCK_COMMENT;} }
	<<EOF>> {
		yybegin(YYINITIAL);
		zzStartRead = commentStart;
		return BLOCK_COMMENT;
	}
	[^/\*]+ { }
	\/[^\*]+ { }
	\*[^\/]+ { }
}

"/*" { yybegin(INSIDE_COMMENT); commentDepth = 1; commentStart = getTokenStart(); }
-?{INTEGER} { return NUMBER; }
- { return MINUS; }
PUSH { return PUSH_TOKEN; }
PEEK { return PEEK_TOKEN; }
PEEK_ALL { return PEEK_ALL_TOKEN; }
POP { return POP_TOKEN; }
POP_ALL { return POP_ALL_TOKEN; }
ANY { return ANY_TOKEN; }
EOI { return EOI_TOKEN; }
SOI { return SOI_TOKEN; }
DROP { return DROP_TOKEN; }
ASCII { return ASCII_TOKEN; }
NEWLINE { return NEWLINE_TOKEN; }
COMMENT { return COMMENT_TOKEN; }
WHITESPACE { return WHITESPACE_TOKEN; }
LETTER { return LETTER_TOKEN; }
CASED_LETTER { return CASED_LETTER_TOKEN; }
UPPERCASE_LETTER { return UPPERCASE_LETTER_TOKEN; }
LOWERCASE_LETTER { return LOWERCASE_LETTER_TOKEN; }
TITLECASE_LETTER { return TITLECASE_LETTER_TOKEN; }
MODIFIER_LETTER { return MODIFIER_LETTER_TOKEN; }
OTHER_LETTER { return OTHER_LETTER_TOKEN; }
MARK { return MARK_TOKEN; }
NONSPACING_MARK { return NONSPACING_MARK_TOKEN; }
SPACING_MARK { return SPACING_MARK_TOKEN; }
ENCLOSING_MARK { return ENCLOSING_MARK_TOKEN; }
NUMBER { return NUMBER_TOKEN; }
DECIMAL_NUMBER { return DECIMAL_NUMBER_TOKEN; }
LETTER_NUMBER { return LETTER_NUMBER_TOKEN; }
OTHER_NUMBER { return OTHER_NUMBER_TOKEN; }
PUNCTUATION { return PUNCTUATION_TOKEN; }
CONNECTOR_PUNCTUATION { return CONNECTOR_PUNCTUATION_TOKEN; }
DASH_PUNCTUATION { return DASH_PUNCTUATION_TOKEN; }
OPEN_PUNCTUATION { return OPEN_PUNCTUATION_TOKEN; }
CLOSE_PUNCTUATION { return CLOSE_PUNCTUATION_TOKEN; }
INITIAL_PUNCTUATION { return INITIAL_PUNCTUATION_TOKEN; }
FINAL_PUNCTUATION { return FINAL_PUNCTUATION_TOKEN; }
OTHER_PUNCTUATION { return OTHER_PUNCTUATION_TOKEN; }
SYMBOL { return SYMBOL_TOKEN; }
MATH_SYMBOL { return MATH_SYMBOL_TOKEN; }
CURRENCY_SYMBOL { return CURRENCY_SYMBOL_TOKEN; }
MODIFIER_SYMBOL { return MODIFIER_SYMBOL_TOKEN; }
OTHER_SYMBOL { return OTHER_SYMBOL_TOKEN; }
SEPARATOR { return SEPARATOR_TOKEN; }
SPACE_SEPARATOR { return SPACE_SEPARATOR_TOKEN; }
LINE_SEPARATOR { return LINE_SEPARATOR_TOKEN; }
PARAGRAPH_SEPARATOR { return PARAGRAPH_SEPARATOR_TOKEN; }
OTHER { return OTHER_TOKEN; }
CONTROL { return CONTROL_TOKEN; }
FORMAT { return FORMAT_TOKEN; }
SURROGATE { return SURROGATE_TOKEN; }
PRIVATE_USE { return PRIVATE_USE_TOKEN; }
UNASSIGNED { return UNASSIGNED_TOKEN; }
ALPHABETIC { return ALPHABETIC_TOKEN; }
BIDI_CONTROL { return BIDI_CONTROL_TOKEN; }
CASE_IGNORABLE { return CASE_IGNORABLE_TOKEN; }
CASED { return CASED_TOKEN; }
CHANGES_WHEN_CASEFOLDED { return CHANGES_WHEN_CASEFOLDED_TOKEN; }
CHANGES_WHEN_CASEMAPPED { return CHANGES_WHEN_CASEMAPPED_TOKEN; }
CHANGES_WHEN_LOWERCASED { return CHANGES_WHEN_LOWERCASED_TOKEN; }
CHANGES_WHEN_TITLECASED { return CHANGES_WHEN_TITLECASED_TOKEN; }
CHANGES_WHEN_UPPERCASED { return CHANGES_WHEN_UPPERCASED_TOKEN; }
DASH { return DASH_TOKEN; }
DEFAULT_IGNORABLE_CODE_POINT { return DEFAULT_IGNORABLE_CODE_POINT_TOKEN; }
DEPRECATED { return DEPRECATED_TOKEN; }
DIACRITIC { return DIACRITIC_TOKEN; }
EXTENDER { return EXTENDER_TOKEN; }
GRAPHEME_BASE { return GRAPHEME_BASE_TOKEN; }
GRAPHEME_EXTEND { return GRAPHEME_EXTEND_TOKEN; }
GRAPHEME_LINK { return GRAPHEME_LINK_TOKEN; }
HEX_DIGIT { return HEX_DIGIT_TOKEN; }
HYPHEN { return HYPHEN_TOKEN; }
IDS_BINARY_OPERATOR { return IDS_BINARY_OPERATOR_TOKEN; }
IDS_TRINARY_OPERATOR { return IDS_TRINARY_OPERATOR_TOKEN; }
ID_CONTINUE { return ID_CONTINUE_TOKEN; }
ID_START { return ID_START_TOKEN; }
IDEOGRAPHIC { return IDEOGRAPHIC_TOKEN; }
JOIN_CONTROL { return JOIN_CONTROL_TOKEN; }
LOGICAL_ORDER_EXCEPTION { return LOGICAL_ORDER_EXCEPTION_TOKEN; }
LOWERCASE { return LOWERCASE_TOKEN; }
MATH { return MATH_TOKEN; }
NONCHARACTER_CODE_POINT { return NONCHARACTER_CODE_POINT_TOKEN; }
OTHER_ALPHABETIC { return OTHER_ALPHABETIC_TOKEN; }
OTHER_DEFAULT_IGNORABLE_CODE_POINT { return OTHER_DEFAULT_IGNORABLE_CODE_POINT_TOKEN; }
OTHER_GRAPHEME_EXTEND { return OTHER_GRAPHEME_EXTEND_TOKEN; }
OTHER_ID_CONTINUE { return OTHER_ID_CONTINUE_TOKEN; }
OTHER_ID_START { return OTHER_ID_START_TOKEN; }
OTHER_LOWERCASE { return OTHER_LOWERCASE_TOKEN; }
OTHER_MATH { return OTHER_MATH_TOKEN; }
OTHER_UPPERCASE { return OTHER_UPPERCASE_TOKEN; }
PATTERN_SYNTAX { return PATTERN_SYNTAX_TOKEN; }
PATTERN_WHITE_SPACE { return PATTERN_WHITE_SPACE_TOKEN; }
PREPENDED_CONCATENATION_MARK { return PREPENDED_CONCATENATION_MARK_TOKEN; }
QUOTATION_MARK { return QUOTATION_MARK_TOKEN; }
RADICAL { return RADICAL_TOKEN; }
REGIONAL_INDICATOR { return REGIONAL_INDICATOR_TOKEN; }
SENTENCE_TERMINAL { return SENTENCE_TERMINAL_TOKEN; }
SOFT_DOTTED { return SOFT_DOTTED_TOKEN; }
TERMINAL_PUNCTUATION { return TERMINAL_PUNCTUATION_TOKEN; }
UNIFIED_IDEOGRAPH { return UNIFIED_IDEOGRAPH_TOKEN; }
UPPERCASE { return UPPERCASE_TOKEN; }
VARIATION_SELECTOR { return VARIATION_SELECTOR_TOKEN; }
WHITE_SPACE { return WHITE_SPACE_TOKEN; }
XID_CONTINUE { return XID_CONTINUE_TOKEN; }
XID_START { return XID_START_TOKEN; }
PUSH { return PUSH_TOKEN; }
PEEK { return PEEK_TOKEN; }
PEEK_ALL { return PEEK_ALL_TOKEN; }
POP { return POP_TOKEN; }
POP_ALL { return POP_ALL_TOKEN; }
ANY { return ANY_TOKEN; }
EOI { return EOI_TOKEN; }
SOI { return SOI_TOKEN; }
DROP { return DROP_TOKEN; }
ASCII { return ASCII_TOKEN; }
NEWLINE { return NEWLINE_TOKEN; }
ASCII_DIGIT { return ASCII_DIGIT_TOKEN; }
ASCII_ALPHA { return ASCII_ALPHA_TOKEN; }
ASCII_ALPHANUMERIC { return ASCII_ALPHANUMERIC_TOKEN; }
ASCII_NONZERO_DIGIT { return ASCII_NONZERO_DIGIT_TOKEN; }
ASCII_BIN_DIGIT { return ASCII_BIN_DIGIT_TOKEN; }
ASCII_OCT_DIGIT { return ASCII_OCT_DIGIT_TOKEN; }
ASCII_HEX_DIGIT { return ASCII_HEX_DIGIT_TOKEN; }
ASCII_ALPHA_UPPER { return ASCII_ALPHA_UPPER_TOKEN; }
ASCII_ALPHA_LOWER { return ASCII_ALPHA_LOWER_TOKEN; }
"_" { return SILENT_MODIFIER; }
{IDENTIFIER} { return IDENTIFIER_TOKEN; }
"//"[^\r\n]* { return LINE_COMMENT; }
"=" { return ASSIGNMENT_OPERATOR; }
"{" { return OPENING_BRACE; }
"}" { return CLOSING_BRACE; }
"(" { return OPENING_PAREN; }
")" { return CLOSING_PAREN; }
"[" { return OPENING_BRACK; }
"]" { return CLOSING_BRACK; }
"@" { return ATOMIC_MODIFIER; }
"$" { return COMPOUND_ATOMIC_MODIFIER; }
"!" { return NON_ATOMIC_MODIFIER; }
"&" { return POSITIVE_PREDICATE_OPERATOR; }
"~" { return SEQUENCE_OPERATOR; }
"|" { return CHOICE_OPERATOR; }
"?" { return OPTIONAL_OPERATOR; }
"*" { return REPEAT_OPERATOR; }
"+" { return REPEAT_ONCE_OPERATOR; }
"^" { return INSENSITIVE_OPERATOR; }
".." { return RANGE_OPERATOR; }
"," { return COMMA; }
{STRING_LITERAL} { return STRING_TOKEN; }
{CHAR_LITERAL} { return CHAR_TOKEN; }
{STRING_INCOMPLETE} { return STRING_INCOMPLETE; }
{CHAR_INCOMPLETE} { return CHAR_INCOMPLETE; }
{WHITE_SPACE}+ { return WHITE_SPACE; }
[^] { return BAD_CHARACTER; }
