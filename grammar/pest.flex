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
IDENTIFIER=[a-zA-Z][a-zA-Z_0-9]*
STRING_UNICODE=\\((u\{{HEXDIGIT}{2,6}\})|(x{HEXDIGIT}{2}))
STRING_INCOMPLETE=\"([^\"\\]|(\\[^])|{STRING_UNICODE})*
CHAR_INCOMPLETE='([^\\\'\x00-\x1F\x7F]|\\[^\x00-\x1F\x7F]+|{STRING_UNICODE})?
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
