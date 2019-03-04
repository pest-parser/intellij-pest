package rs.pest.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.psi.tree.TokenSet
import rs.pest.PestFile
import rs.pest.PestLanguage
import rs.pest.psi.PestTypes.*

class PestTokenType(debugName: String) : IElementType(debugName, PestLanguage.INSTANCE) {
	companion object Static {
		@JvmField
		val PEST_COMMENT = PestTokenType("comment")
		@JvmField
		val COMMENTS = TokenSet.create(PEST_COMMENT)
		@JvmField
		val STRINGS = TokenSet.create(STRING, STRING_TOKEN, CHARACTER, CHAR_TOKEN)
	}
}

class PestParserDefinition : ParserDefinition {
	private companion object {
		private val FILE = IStubFileElementType<PsiFileStubImpl<PestFile>>(PestLanguage.INSTANCE)
	}

	override fun createParser(project: Project?) = PestParser()
	override fun createLexer(project: Project?) = FlexAdapter(PestLexer())
	override fun createElement(node: ASTNode?): PsiElement = PestTypes.Factory.createElement(node)
	override fun createFile(viewProvider: FileViewProvider) = PestFile(viewProvider)
	override fun getStringLiteralElements() = PestTokenType.STRINGS
	override fun getCommentTokens() = PestTokenType.COMMENTS
	override fun getFileNodeType() = FILE
	// TODO: replace after dropping support for 183
	override fun spaceExistanceTypeBetweenTokens(left: ASTNode?, right: ASTNode?) = ParserDefinition.SpaceRequirements.MAY
}
