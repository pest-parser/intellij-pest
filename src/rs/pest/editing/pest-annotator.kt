package rs.pest.editing

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import rs.pest.PestBundle
import rs.pest.PestHighlighter
import rs.pest.psi.PestCharacter
import rs.pest.psi.impl.PestGrammarRuleMixin
import rs.pest.psi.impl.PestIdentifierMixin

class PestAnnotator : Annotator {
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		when (element) {
			is PestGrammarRuleMixin -> grammarRule(element, holder)
			is PestIdentifierMixin -> identifier(element, holder)
			is PestCharacter -> char(element, holder)
		}
	}

	private fun char(element: PestCharacter, holder: AnnotationHolder) {
		if (element.textLength <= 2) holder.createErrorAnnotation(element, PestBundle.message("pest.annotator.empty-char")).apply {
			textAttributes = PestHighlighter.UNRESOLVED
			highlightType = ProblemHighlightType.ERROR
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
		val nameIdentifier = element.nameIdentifier
		if (nameIdentifier is PestIdentifierMixin) {
			val type = element.type
			holder.createInfoAnnotation(nameIdentifier, type.help()).textAttributes = type.highlight
		} else holder.createInfoAnnotation(nameIdentifier, PestBundle.message("pest.annotator.overwrite"))
	}
}
