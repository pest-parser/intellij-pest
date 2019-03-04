package rs.pest

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import icons.PestIcons
import org.intellij.lang.annotations.Language
import rs.pest.psi.PestTokenType
import rs.pest.psi.PestTypes


object PestHighlighter : SyntaxHighlighter {
	@JvmField val KEYWORD = TextAttributesKey.createTextAttributesKey("PEST_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
	@JvmField val IDENTIFIER = TextAttributesKey.createTextAttributesKey("PEST_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
	@JvmField val NUMBER = TextAttributesKey.createTextAttributesKey("PEST_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
	@JvmField val STRING = TextAttributesKey.createTextAttributesKey("PEST_STRING", DefaultLanguageHighlighterColors.STRING)
	@JvmField val CHAR = TextAttributesKey.createTextAttributesKey("PEST_CHAR", DefaultLanguageHighlighterColors.STRING)
	@JvmField val OPERATOR = TextAttributesKey.createTextAttributesKey("PEST_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
	@JvmField val PAREN = TextAttributesKey.createTextAttributesKey("PEST_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
	@JvmField val BRACE = TextAttributesKey.createTextAttributesKey("PEST_BRACES", DefaultLanguageHighlighterColors.BRACES)
	@JvmField val BRACK = TextAttributesKey.createTextAttributesKey("PEST_BRACKET", DefaultLanguageHighlighterColors.BRACKETS)
	@JvmField val COMMENT = TextAttributesKey.createTextAttributesKey("PEST_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
	@JvmField val BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey("PEST_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)

	@JvmField val KEYWORD_KEY = arrayOf(KEYWORD)
	@JvmField val IDENTIFIER_KEY = arrayOf(IDENTIFIER)
	@JvmField val OPERATOR_KEY = arrayOf(OPERATOR)
	@JvmField val STRING_KEY = arrayOf(STRING)
	@JvmField val CHAR_KEY = arrayOf(CHAR)
	@JvmField val NUMBER_KEY = arrayOf(NUMBER)
	@JvmField val PAREN_KEY = arrayOf(PAREN)
	@JvmField val BRACE_KEY = arrayOf(BRACE)
	@JvmField val BRACK_KEY = arrayOf(BRACK)
	@JvmField val COMMENT_KEY = arrayOf(COMMENT)
	@JvmField val BLOCK_COMMENT_KEY = arrayOf(BLOCK_COMMENT)

	private val KEYWORDS_LIST = listOf(
		PestTypes.PEEK_ALL_TOKEN,
		PestTypes.POP_TOKEN,
		PestTypes.POP_ALL_TOKEN,
		PestTypes.ANY_TOKEN,
		PestTypes.EOI_TOKEN,
		PestTypes.SOI_TOKEN,
		PestTypes.DROP_TOKEN,
		PestTypes.ASCII_TOKEN,
		PestTypes.NEWLINE_TOKEN,
		PestTypes.COMMENT_TOKEN,
		PestTypes.WHITESPACE_TOKEN,
		PestTypes.ASCII_DIGIT_TOKEN,
		PestTypes.ASCII_ALPHA_TOKEN,
		PestTypes.ASCII_ALPHANUMERIC_TOKEN,
		PestTypes.ASCII_NONZERO_DIGIT_TOKEN,
		PestTypes.ASCII_BIN_DIGIT_TOKEN,
		PestTypes.ASCII_OCT_DIGIT_TOKEN,
		PestTypes.ASCII_HEX_DIGIT_TOKEN,
		PestTypes.ASCII_ALPHA_UPPER_TOKEN,
		PestTypes.ASCII_ALPHA_LOWER_TOKEN,
		PestTypes.PUSH_TOKEN,
		PestTypes.PEEK_TOKEN)

	private val OPERATORS_LIST = listOf(
		PestTypes.PUSH_TOKEN,
		PestTypes.PEEK_TOKEN)

	/** brackets */
	private val BRACKS = listOf(PestTypes.OPENING_BRACK, PestTypes.CLOSING_BRACK)
	/** braces */
	private val BRACES = listOf(PestTypes.OPENING_BRACE, PestTypes.CLOSING_BRACE)
	/** parentheses */
	private val PARENS = listOf(PestTypes.OPENING_PAREN, PestTypes.CLOSING_PAREN)

	override fun getHighlightingLexer() = lexer()
	override fun getTokenHighlights(type: IElementType?): Array<TextAttributesKey> = when (type) {
		PestTypes.IDENTIFIER_TOKEN -> IDENTIFIER_KEY
		PestTypes.STRING_TOKEN -> STRING_KEY
		PestTypes.CHAR_TOKEN -> CHAR_KEY
		PestTokenType.LINE_COMMENT -> COMMENT_KEY
		PestTokenType.BLOCK_COMMENT -> BLOCK_COMMENT_KEY
		PestTypes.NUMBER,
		PestTypes.MINUS -> NUMBER_KEY
		in PARENS-> PAREN_KEY
		in BRACKS -> BRACK_KEY
		in BRACES -> BRACE_KEY
		in KEYWORDS_LIST -> KEYWORD_KEY
		in OPERATORS_LIST -> OPERATOR_KEY
		else -> emptyArray()
	}
}

class PestHighlighterFactory : SyntaxHighlighterFactory() {
	override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = PestHighlighter
}

class PestColorSettingsPage : ColorSettingsPage {
	private companion object DescriptorHolder {
		private val DESCRIPTORS = arrayOf(
			AttributesDescriptor(PestBundle.message("pest.highlighter.settings.identifier"), PestHighlighter.IDENTIFIER),
			AttributesDescriptor(PestBundle.message("pest.highlighter.settings.number"), PestHighlighter.NUMBER),
			AttributesDescriptor(PestBundle.message("pest.highlighter.settings.string"), PestHighlighter.STRING),
			AttributesDescriptor(PestBundle.message("pest.highlighter.settings.char"), PestHighlighter.CHAR),
			AttributesDescriptor(PestBundle.message("pest.highlighter.settings.comment"), PestHighlighter.COMMENT),
			AttributesDescriptor(PestBundle.message("pest.highlighter.settings.block-comment"), PestHighlighter.BLOCK_COMMENT))

		private val ADDITIONAL_DESCRIPTORS = mapOf<String,TextAttributesKey>()
	}

	override fun getHighlighter(): SyntaxHighlighter = PestHighlighter
	override fun getAdditionalHighlightingTagToDescriptorMap() = ADDITIONAL_DESCRIPTORS
	override fun getIcon() = PestIcons.PEST
	override fun getAttributeDescriptors() = DESCRIPTORS
	override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
	override fun getDisplayName() = PestFileType.name
	@Language("Pest")
	override fun getDemoText() = """// Syntax Sample
/* Block comment */
rule = { PUSH ("string") ~ other_rule ~ (!"PRE" ~ ) }
"""
}
