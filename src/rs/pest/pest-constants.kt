package rs.pest

import org.jetbrains.annotations.NonNls

@NonNls const val PEST_CONTEXT_ID = "PEST_CONTEXT"
@NonNls const val PEST_LANGUAGE_NAME = "Pest"
@NonNls const val PEST_EXTENSION = "pest"
@NonNls const val PEST_BLOCK_COMMENT_BEGIN = "/*"
@NonNls const val PEST_BLOCK_COMMENT_END = "*/"
@NonNls const val PEST_MODULE_ID = "PEST_MODULE_TYPE"
@NonNls const val PEST_RUN_CONFIG_ID = "PEST_RUN_CONFIG_ID"
@NonNls const val PEST_PLUGIN_ID = "rs.pest"

@NonNls const val PEST_WEBSITE = "https://pest.rs/"

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
	"COMMENT",
	"WHITESPACE",
	"ASCII_DIGIT",
	"ASCII_ALPHA",
	"ASCII_ALPHANUMERIC",
	"ASCII_NONZERO_DIGIT",
	"ASCII_BIN_DIGIT",
	"ASCII_OCT_DIGIT",
	"ASCII_HEX_DIGIT",
	"ASCII_ALPHA_UPPER",
	"ASCII_ALPHA_LOWER")
