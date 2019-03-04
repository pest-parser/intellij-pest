package rs.pest.psi

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import rs.pest.PestBundle
import rs.pest.PestHighlighter
import rs.pest.PestLanguage

class PestElementType(debugName: String) : IElementType(debugName, PestLanguage.INSTANCE)

class PestTokenType(debugName: String) : IElementType(debugName, PestLanguage.INSTANCE) {
	companion object Builtin {
		@JvmField
		val LINE_COMMENT = PestTokenType("line comment")
		@JvmField
		val BLOCK_COMMENT = PestTokenType("block comment")
		@JvmField
		val COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)
		@JvmField
		val STRINGS = TokenSet.create(PestTypes.STRING, PestTypes.STRING_TOKEN, PestTypes.CHARACTER, PestTypes.CHAR_TOKEN)
		@JvmField
		val IDENTIFIERS = TokenSet.create(PestTypes.IDENTIFIER)

		fun fromText(text: String, project: Project) = PsiFileFactory.getInstance(project).createFileFromText(PestLanguage.INSTANCE, text).firstChild
	}
}

enum class PestRuleType(val highlight: TextAttributesKey) {
	Simple(PestHighlighter.SIMPLE),
	Silent(PestHighlighter.SILENT),
	Atomic(PestHighlighter.ATOMIC),
	CompoundAtomic(PestHighlighter.COMPOUND_ATOMIC),
	NonAtomic(PestHighlighter.NON_ATOMIC);

	fun help() = when (this) {
		Simple -> PestBundle.message("pest.rule.help.simple")
		Silent -> PestBundle.message("pest.rule.help.silent")
		Atomic -> PestBundle.message("pest.rule.help.atomic")
		CompoundAtomic -> PestBundle.message("pest.rule.help.compound")
		NonAtomic -> PestBundle.message("pest.rule.help.non-atomic")
	}
}
