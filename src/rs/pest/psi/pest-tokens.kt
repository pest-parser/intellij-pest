package rs.pest.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import rs.pest.PestLanguage
import rs.pest.psi.PestTypes.*

class PestTokenType(debugName: String) : IElementType(debugName, PestLanguage.INSTANCE) {
	companion object Static {
		@JvmField val PEST_COMMENT = PestTokenType("comment")
		@JvmField val COMMENTS = TokenSet.create(PEST_COMMENT)
		@JvmField val STRINGS = TokenSet.create(PEST_STRING, PEST_STRING_TOKEN, PEST_CHARACTER, PEST_CHAR_TOKEN)
	}
}
