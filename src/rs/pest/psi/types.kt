package rs.pest.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import rs.pest.PestLanguage

class PestElementType(debugName: String) : IElementType(debugName, PestLanguage.INSTANCE)

class PestTokenType(debugName: String) : IElementType(debugName, PestLanguage.INSTANCE) {
	companion object Builtin {
		@JvmField val LINE_COMMENT = PestTokenType("line comment")
		@JvmField val BLOCK_COMMENT = PestTokenType("block comment")
		@JvmField val COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)
		@JvmField val STRINGS = TokenSet.create(PestTypes.STRING, PestTypes.STRING_TOKEN, PestTypes.CHARACTER, PestTypes.CHAR_TOKEN)
	}
}
