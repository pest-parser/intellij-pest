package rs.pest

import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls

@NonNls const val PEST_DEFAULT_CONTEXT_ID = "PEST_DEFAULT_CONTEXT"
@NonNls const val PEST_LOCAL_CONTEXT_ID = "PEST_LOCAL_CONTEXT"
@Nls const val PEST_LOCAL_CONTEXT_NAME = "Expression"
@NonNls const val PEST_LANGUAGE_NAME = "Pest"
@NonNls const val PEST_EXTENSION = "pest"
@NonNls const val PEST_BLOCK_COMMENT_BEGIN = "/*"
@NonNls const val PEST_BLOCK_COMMENT_END = "*/"
@NonNls const val PEST_LINE_COMMENT = "// "
@NonNls const val PEST_RUN_CONFIG_ID = "PEST_RUN_CONFIG_ID"
@NonNls const val PEST_PLUGIN_ID = "rs.pest"

@NonNls const val PEST_WEBSITE = "https://pest.rs/"
@NonNls const val PEST_FOLDING_PLACEHOLDER = "{...}"

@JvmField val BUILTIN_RULES = listOf(
	"PUSH",
	"PEEK",
	"PEEK_ALL",
	"POP",
	"POP_ALL",
	"ANY",
	"EOI",
	"SOI",
	"DROP",
	"ASCII",
	"NEWLINE",
	"ASCII_DIGIT",
	"ASCII_ALPHA",
	"ASCII_ALPHANUMERIC",
	"ASCII_NONZERO_DIGIT",
	"ASCII_BIN_DIGIT",
	"ASCII_OCT_DIGIT",
	"ASCII_HEX_DIGIT",
	"ASCII_ALPHA_UPPER",
	"ASCII_ALPHA_LOWER")

@JvmField val BUILTIN_RULE_FOR_COMPLETION = BUILTIN_RULES + listOf("COMMENT", "WHITESPACE")
