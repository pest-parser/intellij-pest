package rs.pest.editing

import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import rs.pest.PEST_BLOCK_COMMENT_BEGIN
import rs.pest.PEST_BLOCK_COMMENT_END
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
