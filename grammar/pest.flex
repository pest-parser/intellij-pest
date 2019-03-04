package rs.pest.psi;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static rs.pest.psi.PestTokenType.PEST_COMMENT;
import static rs.pest.psi.PestTypes.*;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;

%%

%{
  public PestLexer() { this((java.io.Reader)null); }

  private int nestedLeftComment = 0;
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
IDENTIFIER_CHAR=[a-zA-Z_0-9]
INTEGER=[0-9]+
IDENTIFIER={IDENTIFIER_CHAR}+
STRING_LITERAL=\"([^\"\\]|(\\[^])|{STRING_UNICODE})*\"
STRING_UNICODE=\\((u\{{HEXDIGIT}{2,6}\})|(x{HEXDIGIT}{2}))
CHAR_LITERAL='([^\\\'\x00-\x1F\x7F]|\\[^\x00-\x1F\x7F]+|{STRING_UNICODE})'
HEXDIGIT=[a-fA-F0-9]

%%

<INSIDE_COMMENT> {
	"/*" { ++nestedLeftComment; }
	"*/" { if (--nestedLeftComment <= 0) {yybegin(YYINITIAL); return PEST_COMMENT;} }
	[^\*]+ { }
	[^/]+ { }
	\/[^\*]+ { }
	\*[^\/]+ { }
}

"/*" { yybegin(INSIDE_COMMENT); nestedLeftComment = 0; }
-?{INTEGER} { return PEST_NUMBER; }
- { return PEST_MINUS; }
PUSH { return PEST_PUSH_TOKEN; }
PEEK { return PEST_PEEK_TOKEN; }
"_" { return PEST_SILENT_MODIFIER; }
{IDENTIFIER} { return PEST_IDENTIFIER_TOKEN; }
"//"[^\r\n]]* { yybegin(YYINITIAL); }
"=" { return PEST_ASSIGNMENT_OPERATOR; }
"{" { return PEST_OPENING_BRACE; }
"}" { return PEST_CLOSING_BRACE; }
"(" { return PEST_OPENING_PAREN; }
")" { return PEST_CLOSING_PAREN; }
"[" { return PEST_OPENING_BRACK; }
"]" { return PEST_CLOSING_BRACK; }
"@" { return PEST_ATOMIC_MODIFIER; }
"$" { return PEST_COMPOUND_ATOMIC_MODIFIER; }
"!" { return PEST_NON_ATOMIC_MODIFIER; }
"&" { return PEST_POSITIVE_PREDICATE_OPERATOR; }
"~" { return PEST_SEQUENCE_OPERATOR; }
"|" { return PEST_CHOICE_OPERATOR; }
"?" { return PEST_OPTIONAL_OPERATOR; }
"*" { return PEST_REPEAT_OPERATOR; }
"+" { return PEST_REPEAT_ONCE_OPERATOR; }
"^" { return PEST_INSENSITIVE_OPERATOR; }
{STRING_LITERAL} { return PEST_STRING_TOKEN; }
{CHAR_LITERAL} { return PEST_CHAR_TOKEN; }
{WHITE_SPACE} { return WHITE_SPACE; }
[^] { return BAD_CHARACTER; }
