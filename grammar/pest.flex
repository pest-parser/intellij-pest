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
-?{INTEGER} { return NUMBER; }
- { return MINUS; }
PUSH { return PUSH_TOKEN; }
PEEK { return PEEK_TOKEN; }
"_" { return SILENT_MODIFIER; }
{IDENTIFIER} { return IDENTIFIER_TOKEN; }
"//"[^\r\n]]* { yybegin(YYINITIAL); }
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
{STRING_LITERAL} { return STRING_TOKEN; }
{CHAR_LITERAL} { return CHAR_TOKEN; }
{WHITE_SPACE} { return WHITE_SPACE; }
[^] { return BAD_CHARACTER; }
