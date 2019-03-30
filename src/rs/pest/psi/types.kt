package rs.pest.psi

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import rs.pest.PestBundle
import rs.pest.PestHighlighter
import rs.pest.PestLanguage
import rs.pest.psi.impl.PestGrammarRuleMixin

class PestElementType(debugName: String) : IElementType(debugName, PestLanguage.INSTANCE)

class PestTokenType(debugName: String) : IElementType(debugName, PestLanguage.INSTANCE) {
	companion object Builtin {
		@JvmField val LINE_COMMENT = PestTokenType("line comment")
		@JvmField val BLOCK_COMMENT = PestTokenType("block comment")
		@JvmField val STRING_INCOMPLETE = PestTokenType("incomplete string")
		@JvmField val CHAR_INCOMPLETE = PestTokenType("incomplete char")
		@JvmField val COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)
		@JvmField val STRINGS = TokenSet.create(PestTypes.STRING_TOKEN, PestTypes.CHAR_TOKEN)
		@JvmField val WHITE_SPACE = TokenSet.create(TokenType.WHITE_SPACE)
		@JvmField val INCOMPLETE_STRINGS = TokenSet.create(STRING_INCOMPLETE, CHAR_INCOMPLETE)
		@JvmField val ANY_STRINGS = TokenSet.orSet(STRINGS, INCOMPLETE_STRINGS)
		@JvmField val IDENTIFIERS = TokenSet.create(PestTypes.IDENTIFIER)

		fun fromText(text: String, project: Project): PsiElement? = PsiFileFactory.getInstance(project).createFileFromText(PestLanguage.INSTANCE, text).firstChild
		fun createRule(text: String, project: Project) = fromText(text, project) as? PestGrammarRuleMixin
		fun createBody(text: String, project: Project) = createRule("r=$text", project)?.grammarBody as? PestGrammarBody
		fun createExpression(text: String, project: Project) = createBody("{$text}", project)?.expression
		fun createRuleName(text: String, project: Project) = createRule("$text = {\"d\"}", project)?.nameIdentifier
	}
}

enum class PestRuleType(val description: String, val highlight: TextAttributesKey) {
	Simple("Simple", PestHighlighter.SIMPLE),
	Silent("Silent", PestHighlighter.SILENT),
	Atomic("Atomic", PestHighlighter.ATOMIC),
	CompoundAtomic("Compound atomic", PestHighlighter.COMPOUND_ATOMIC),
	NonAtomic("Non-atomic", PestHighlighter.NON_ATOMIC);

	fun help() = when (this) {
		Simple -> PestBundle.message("pest.rule.help.simple")
		Silent -> PestBundle.message("pest.rule.help.silent")
		Atomic -> PestBundle.message("pest.rule.help.atomic")
		CompoundAtomic -> PestBundle.message("pest.rule.help.compound")
		NonAtomic -> PestBundle.message("pest.rule.help.non-atomic")
	}
}
