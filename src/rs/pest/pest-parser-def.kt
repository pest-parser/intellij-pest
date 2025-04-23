package rs.pest

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import rs.pest.psi.PestLexer
import rs.pest.psi.PestParser
import rs.pest.psi.PestTokenType
import rs.pest.psi.PestTypes

fun lexer() = FlexAdapter(PestLexer())

class PestParserDefinition : ParserDefinition {
	private companion object {
		private val FILE = IStubFileElementType<PsiFileStubImpl<PestFile>>(PestLanguage.INSTANCE)
	}

	override fun createParser(project: Project?) = PestParser()
	override fun createLexer(project: Project?) = lexer()
	override fun createElement(node: ASTNode?): PsiElement = PestTypes.Factory.createElement(node)
	override fun createFile(viewProvider: FileViewProvider) = PestFile(viewProvider)
	override fun getStringLiteralElements() = PestTokenType.STRINGS
	override fun getWhitespaceTokens() = PestTokenType.WHITE_SPACE
	override fun getCommentTokens() = PestTokenType.COMMENTS
	override fun getFileNodeType() = FILE
}
