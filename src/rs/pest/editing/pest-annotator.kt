package rs.pest.editing

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import rs.pest.PestBundle
import rs.pest.PestHighlighter
import rs.pest.psi.impl.PestGrammarRuleMixin
import rs.pest.psi.impl.PestIdentifierMixin

class PestAnnotator : Annotator {
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		when (element) {
			is PestGrammarRuleMixin -> grammarRule(element, holder)
			is PestIdentifierMixin -> identifier(element, holder)
		}
	}

	private fun identifier(element: PestIdentifierMixin, holder: AnnotationHolder) {
		val rule = element.reference.resolve() as? PestGrammarRuleMixin ?: run {
			holder.createErrorAnnotation(element, PestBundle.message("pest.annotator.unresolved")).run {
				highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
				textAttributes = PestHighlighter.UNRESOLVED
			}
			return
		}
		holder.createInfoAnnotation(element, null).textAttributes = rule.type.highlight
	}

	private fun grammarRule(element: PestGrammarRuleMixin, holder: AnnotationHolder) {
		element.nameIdentifier?.let {
			val type = element.type
			holder.createInfoAnnotation(it, type.help()).textAttributes = type.highlight
		} ?: holder.createInfoAnnotation(element, PestBundle.message("pest.annotator.overwrite"))
	}
}
