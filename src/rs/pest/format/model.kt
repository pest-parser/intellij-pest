package rs.pest.format

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import rs.pest.PestLanguage
import rs.pest.psi.PestTypes

class PestFormattingModelBuilder : FormattingModelBuilder {
	override fun createModel(formattingContext: FormattingContext): FormattingModel {
		return FormattingModelProvider
			.createFormattingModelForPsiFile(
				formattingContext.containingFile,
				PestSimpleBlock(
					createSpaceBuilder(formattingContext.codeStyleSettings),
					formattingContext.node,
					Wrap.createWrap(WrapType.NONE, false),
					Alignment.createAlignment(true)
				),
				formattingContext.codeStyleSettings
			)
	}

	private fun createSpaceBuilder(settings: CodeStyleSettings) = SpacingBuilder(settings, PestLanguage.INSTANCE)
		.around(PestTypes.CHOICE_OPERATOR).spaces(1)
		.around(PestTypes.SEQUENCE_OPERATOR).spaces(1)
		.around(PestTypes.ASSIGNMENT_OPERATOR).spaces(1)
		.before(PestTypes.OPENING_PAREN).spaces(1)
		.before(PestTypes.OPENING_BRACE).spaces(1)
		.after(PestTypes.OPTIONAL_OPERATOR).spaces(1)
		.after(PestTypes.REPEAT_OPERATOR).spaces(1)
		.after(PestTypes.REPEAT_ONCE_OPERATOR).spaces(1)
		.after(PestTypes.CLOSING_PAREN).spaces(1)
		.after(PestTypes.CLOSING_BRACE).none()

	override fun getRangeAffectingIndent(file: PsiFile, offset: Int, elementAtOffset: ASTNode): TextRange? = null
}
