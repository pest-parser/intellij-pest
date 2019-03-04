package rs.pest.editing

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.cache.impl.BaseFilterLexer
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer
import com.intellij.psi.impl.cache.impl.id.LexerBasedIdIndexer
import com.intellij.psi.impl.cache.impl.todo.LexerBasedTodoIndexer
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import rs.pest.*
import rs.pest.psi.PestGrammarRule
import rs.pest.psi.PestTokenType
import rs.pest.psi.PestTypes

class PestCommenter : Commenter {
	override fun getCommentedBlockCommentPrefix() = blockCommentPrefix
	override fun getCommentedBlockCommentSuffix() = blockCommentSuffix
	override fun getBlockCommentPrefix() = PEST_BLOCK_COMMENT_BEGIN
	override fun getBlockCommentSuffix() = PEST_BLOCK_COMMENT_END
	override fun getLineCommentPrefix() = "// "
}

class PestBraceMatcher : PairedBraceMatcher {
	private companion object Pairs {
		private val PAIRS = arrayOf(
			BracePair(PestTypes.OPENING_BRACK, PestTypes.CLOSING_BRACK, false),
			BracePair(PestTypes.OPENING_PAREN, PestTypes.CLOSING_PAREN, false),
			BracePair(PestTypes.OPENING_BRACE, PestTypes.CLOSING_BRACE, false))
	}

	override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) = openingBraceOffset
	override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true
	override fun getPairs() = PAIRS
}


class PestTodoIndexer : LexerBasedTodoIndexer() {
	override fun createLexer(consumer: OccurrenceConsumer): Lexer = PestIdIndexer.createIndexingLexer(consumer)
}

class PestIdIndexer : LexerBasedIdIndexer() {
	override fun createLexer(consumer: OccurrenceConsumer): Lexer {
		return createIndexingLexer(consumer)
	}

	companion object {
		fun createIndexingLexer(consumer: OccurrenceConsumer): Lexer {
			return PestFilterLexer(lexer(), consumer)
		}
	}
}

class PestFilterLexer(originalLexer: Lexer, table: OccurrenceConsumer) : BaseFilterLexer(originalLexer, table) {
	override fun advance() {
		scanWordsInToken(UsageSearchContext.IN_COMMENTS.toInt(), false, false)
		advanceTodoItemCountsInToken()
		myDelegate.advance()
	}
}

class PestTodoIndexPatternBuilder : IndexPatternBuilder {
	override fun getIndexingLexer(file: PsiFile): Lexer? = if (file is PestFile) lexer() else null
	override fun getCommentTokenSet(file: PsiFile): TokenSet? = if (file is PestFile) PestTokenType.COMMENTS else null
	override fun getCommentStartDelta(tokenType: IElementType?) = 0
	override fun getCommentEndDelta(tokenType: IElementType?) = 0
}

class PestFindUsagesProvider : FindUsagesProvider {
	override fun canFindUsagesFor(element: PsiElement) = element is PsiNameIdentifierOwner
	override fun getHelpId(psiElement: PsiElement): String? = null
	override fun getType(element: PsiElement) = if (element is PestGrammarRule) PestBundle.message("pest.rule.name") else ""
	override fun getDescriptiveName(element: PsiElement) = (element as? PsiNamedElement)?.name ?: ""
	override fun getNodeText(element: PsiElement, useFullName: Boolean) = getDescriptiveName(element)
	override fun getWordsScanner() = DefaultWordsScanner(lexer(), PestTokenType.IDENTIFIERS, PestTokenType.COMMENTS, PestTokenType.STRINGS)
}

class PestRuleNameValidator : NamesValidator {
	override fun isKeyword(name: String, project: Project?) = "?!@#$%^&*()[]{}<>,./|\\~`'\" \r\n\t".any { it in name } || name in BUILTIN_RULES
	override fun isIdentifier(name: String, project: Project?) = !isKeyword(name, project)
}

class PestRefactoringSupportProvider : RefactoringSupportProvider() {
	override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?) = true
}

class PestPairBackspaceHandler : BackspaceHandlerDelegate() {
	override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
		if (c !in "\"`'(" || file !is PestFile || !CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return
		val offset = editor.caretModel.offset
		val highlighter = (editor as EditorEx).highlighter
		val iterator = highlighter.createIterator(offset)
		if (iterator.tokenType != PestTypes.CLOSING_BRACK
			&& iterator.tokenType != PestTypes.CLOSING_BRACE
			&& iterator.tokenType != PestTypes.CLOSING_PAREN) return
		iterator.retreat()
		if (iterator.tokenType != PestTypes.OPENING_BRACK
			&& iterator.tokenType != PestTypes.OPENING_BRACE
			&& iterator.tokenType != PestTypes.OPENING_PAREN) return

		if (offset + 1 > file.textLength) editor.document.deleteString(offset, offset)
		else editor.document.deleteString(offset, offset + 1)
	}

	override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
		return false
	}
}
