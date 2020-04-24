package rs.pest.editing

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import rs.pest.PestBundle
import rs.pest.PestFile
import rs.pest.PestHighlighter
import rs.pest.psi.PestCharacter
import rs.pest.psi.impl.PestFixedBuiltinRuleNameMixin
import rs.pest.psi.impl.PestGrammarRuleMixin
import rs.pest.psi.impl.PestIdentifierMixin
import rs.pest.psi.impl.PestRuleNameMixin

class PestAnnotator : Annotator {
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		when (element) {
			is PestFile -> file(element, holder)
			is PestIdentifierMixin -> identifier(element, holder)
			is PestCharacter -> char(element, holder)
			is PestFixedBuiltinRuleNameMixin -> fixedBuiltInRuleName(element, holder)
			is PestRuleNameMixin -> validRuleName(element, holder)
		}
	}

	private fun file(element: PestFile, holder: AnnotationHolder) {
		element.errors.forEach { (range, msg) ->
			holder.newAnnotation(HighlightSeverity.ERROR, msg)
					.range(range)
					.create()
		}
	}

	private fun char(element: PestCharacter, holder: AnnotationHolder) {
		if (element.textLength <= 2) holder
				.newAnnotation(HighlightSeverity.ERROR, PestBundle.message("pest.annotator.empty-char"))
				.range(element)
				.textAttributes(PestHighlighter.UNRESOLVED)
				.highlightType(ProblemHighlightType.ERROR)
				.create()
	}

	private fun fixedBuiltInRuleName(element: PestFixedBuiltinRuleNameMixin, holder: AnnotationHolder) {
		holder.newAnnotation(HighlightSeverity.ERROR, PestBundle.message("pest.annotator.overwrite"))
				.range(element)
				.highlightType(ProblemHighlightType.ERROR)
				.create()
	}

	private fun validRuleName(element: PestRuleNameMixin, holder: AnnotationHolder) {
		val rule = element.parent as PestGrammarRuleMixin
		holder.newAnnotation(HighlightSeverity.INFORMATION, rule.type.help())
				.textAttributes(rule.type.highlight)
				.range(element)
				.create()
		if (PsiTreeUtil.hasErrorElements(element.parent)) {
			holder.newAnnotation(HighlightSeverity.ERROR, PestBundle.message("pest.annotator.rule-contains-error", element.text))
					.highlightType(ProblemHighlightType.GENERIC_ERROR)
					.range(element)
					.create()
		}
	}

	private fun identifier(element: PestIdentifierMixin, holder: AnnotationHolder) {
		val rule = element.reference.resolve() as? PestGrammarRuleMixin ?: run {
			holder.newAnnotation(HighlightSeverity.ERROR, PestBundle.message("pest.annotator.unresolved"))
					.range(element)
					.highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
					.textAttributes(PestHighlighter.UNRESOLVED)
					.create()
			return
		}
		holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
				.textAttributes(rule.type.highlight)
				.range(element)
				.create()
	}
}
