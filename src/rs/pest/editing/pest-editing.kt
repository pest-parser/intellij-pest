package rs.pest.editing

import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.lexer.Lexer
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.cache.impl.BaseFilterLexer
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer
import com.intellij.psi.impl.cache.impl.id.LexerBasedIdIndexer
import com.intellij.psi.impl.cache.impl.todo.LexerBasedTodoIndexer
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import rs.pest.PEST_BLOCK_COMMENT_BEGIN
import rs.pest.PEST_BLOCK_COMMENT_END
import rs.pest.PestFile
import rs.pest.lexer
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
