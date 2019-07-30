package rs.pest.format

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import rs.pest.psi.childrenWithLeaves

class PestSimpleBlock(
	private val spacing: SpacingBuilder,
	node: ASTNode,
	wrap: Wrap?,
	alignment: Alignment?
) : AbstractBlock(node, wrap, alignment) {
	override fun getSpacing(lhs: Block?, rhs: Block) = spacing.getSpacing(this, lhs, rhs)
	override fun isLeaf() = node.firstChildNode == null
	override fun getIndent() = Indent.getNoneIndent()
	override fun buildChildren(): MutableList<Block> = node
		.childrenWithLeaves
		.filter { it.elementType != TokenType.WHITE_SPACE }
		.map { PestSimpleBlock(spacing, it, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment()) }
		.toMutableList()
}
