package rs.pest.editing

import com.intellij.lang.Commenter
import rs.pest.PEST_BLOCK_COMMENT_BEGIN
import rs.pest.PEST_BLOCK_COMMENT_END

class PestCommenter : Commenter {
	override fun getCommentedBlockCommentPrefix() = blockCommentPrefix
	override fun getCommentedBlockCommentSuffix() = blockCommentSuffix
	override fun getBlockCommentPrefix() = PEST_BLOCK_COMMENT_BEGIN
	override fun getBlockCommentSuffix() = PEST_BLOCK_COMMENT_END
	override fun getLineCommentPrefix() = "// "
}
