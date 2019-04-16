package rs.pest.editing

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.lang.annotations.Language
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

	@Language("RegExp")
	private val errorMsgRegex = Regex("\\A(\\d+)\\^(\\d+)\\^(\\d+)\\^(\\d+)\\^(.*)$")

	private fun vmListener(element: PestFile, holder: AnnotationHolder) = object : DocumentListener {
		override fun documentChanged(event: DocumentEvent) {
			if (PsiTreeUtil.hasErrorElements(element)) return
			element.rebootVM()
			val (works, messages) = element.reloadVM()
			val project = element.project
			val dom = PsiDocumentManager.getInstance(project).getDocument(element) ?: return
			element.errors = if (works) {
				emptyList()
			} else messages.mapNotNull { errorMsgRegex.matchEntire(it)?.groupValues }.map {
				val startLine = it[1].toInt() - 1
				val startCol = it[2].toInt() - 1
				val endLine = it[3].toInt() - 1
				val endCol = it[4].toInt() - 1
				val range = TextRange(dom.getLineStartOffset(startLine) + startCol, dom.getLineStartOffset(endLine) + endCol)
				Pair(range, it[5])
			}.toList()
		}
	}

	private fun file(element: PestFile, holder: AnnotationHolder) {
		val project = element.project
		if (!element.isDocumentListenerAdded) {
			element.isDocumentListenerAdded = true
			PsiDocumentManager.getInstance(project).getDocument(element)?.addDocumentListener(vmListener(element, holder))
		}
		val errors = element.errors
		if (errors.isNotEmpty()) errors.forEach { (range, msg) -> holder.createErrorAnnotation(range, msg) }
	}

	private fun char(element: PestCharacter, holder: AnnotationHolder) {
		if (element.textLength <= 2) holder.createErrorAnnotation(element, PestBundle.message("pest.annotator.empty-char")).apply {
			textAttributes = PestHighlighter.UNRESOLVED
			highlightType = ProblemHighlightType.ERROR
		}
	}

	private fun fixedBuiltInRuleName(element: PestFixedBuiltinRuleNameMixin, holder: AnnotationHolder) {
		holder.createErrorAnnotation(element, PestBundle.message("pest.annotator.overwrite")).run {
			highlightType = ProblemHighlightType.ERROR
		}
	}

	private fun validRuleName(element: PestRuleNameMixin, holder: AnnotationHolder) {
		val rule = element.parent as PestGrammarRuleMixin
		holder.createInfoAnnotation(element, rule.type.help()).textAttributes = rule.type.highlight
		if (PsiTreeUtil.hasErrorElements(element.parent)) {
			holder.createErrorAnnotation(element, PestBundle.message("pest.annotator.rule-contains-error", element.text)).run {
				highlightType = ProblemHighlightType.GENERIC_ERROR
			}
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
}
