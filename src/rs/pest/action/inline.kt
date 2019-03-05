package rs.pest.action

import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.inline.InlineOptionsDialog
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewNodeTextLocation
import rs.pest.PestBundle
import rs.pest.PestLanguage
import rs.pest.psi.*
import rs.pest.psi.impl.PestGrammarRuleMixin
import rs.pest.psi.impl.bodyText

class PestInlineRuleActionHandler : InlineActionHandler() {
	override fun isEnabledForLanguage(l: Language?) = l == PestLanguage.INSTANCE
	override fun canInlineElement(element: PsiElement?) = element is PestGrammarRuleMixin && !element.isRecursive
	override fun inlineElement(project: Project, editor: Editor?, element: PsiElement?) {
		val rule = element as? PestGrammarRuleMixin ?: return
		if (PsiTreeUtil.hasErrorElements(rule)) {
			CommonRefactoringUtil.showErrorHint(project, editor, PestBundle.message("pest.actions.inline.has-error.info"), PestBundle.message("pest.actions.inline.error.title"), null)
			return
		}
		if (rule.isRecursive) {
			CommonRefactoringUtil.showErrorHint(project, editor, PestBundle.message("pest.actions.inline.recursive.info"), PestBundle.message("pest.actions.inline.error.title"), null)
			return
		}
		if (!CommonRefactoringUtil.checkReadOnlyStatus(project, rule)) return
		val reference = editor?.let { rule.containingFile.findElementAt(it.caretModel.offset) }
		PestInlineDialog(project, element, reference).show()
	}
}

class PestInlineViewDescriptor(private val element: PestGrammarRuleMixin) : UsageViewDescriptor {
	override fun getElements() = arrayOf(element)
	override fun getProcessedElementsHeader() = PestBundle.message("pest.actions.inline.view.title")
	override fun getCodeReferencesText(usagesCount: Int, filesCount: Int): String =
		RefactoringBundle.message("invocations.to.be.inlined", UsageViewBundle.getReferencesString(usagesCount, filesCount))

	override fun getCommentReferencesText(usagesCount: Int, filesCount: Int): String =
		RefactoringBundle.message("comments.elements.header", UsageViewBundle.getOccurencesString(usagesCount, filesCount))
}

class PestInlineDialog(project: Project, val element: PestGrammarRuleMixin, private val reference: PsiElement?)
	: InlineOptionsDialog(project, true, element) {
	init {
		myInvokedOnReference = reference != null
		init()
	}

	override fun isInlineThis() = false
	override fun getNameLabelText() = ElementDescriptionUtil.getElementDescription(element, UsageViewNodeTextLocation.INSTANCE)
	override fun getInlineThisText() = PestBundle.message("pest.actions.inline.dialog.this")
	override fun getInlineAllText() = PestBundle.message("pest.actions.inline.dialog.all")
	override fun getBorderTitle() = PestBundle.message("pest.actions.inline.dialog.title")
	override fun doAction() = invokeRefactoring(PestInlineProcessor(project, element, reference, isInlineThisOnly))
}

class PestInlineProcessor(
	project: Project,
	val rule: PestGrammarRuleMixin,
	private val reference: PsiElement?,
	private val thisOnly: Boolean
) : BaseRefactoringProcessor(project) {
	private val name = rule.name
	override fun getCommandName() = PestBundle.message("pest.actions.inline.command.name", name)
	override fun createUsageViewDescriptor(usages: Array<UsageInfo>) = PestInlineViewDescriptor(rule)
	override fun findUsages() = rule
		.references
		.asSequence()
		.map(PsiReference::getElement)
		.filter(PsiElement::isValid)
		.filterNot { PsiTreeUtil.isAncestor(rule, it, false) }
		.map(::UsageInfo)
		.toList()
		.toTypedArray()

	override fun performRefactoring(usages: Array<UsageInfo>) {
		val grammarBody = rule.grammarBody
		val expression = grammarBody?.expression ?: return
		val newText = when (expression) {
			is PestString -> expression.text
			is PestCharacter -> expression.text
			is PestIdentifier -> expression.text
			is PestTerm -> expression.text
			is PestRange -> expression.text
			is PestBuiltin -> expression.text
			else -> "(${expression.bodyText(grammarBody.textLength).trim()})"
		}
		ApplicationManager.getApplication().runWriteAction {
			val newElement = PestTokenType.createExpression(newText, myProject)
			if (thisOnly) {
				reference?.replace(newElement)
			} else {
				usages.forEach { it.element?.replace(newElement) }
				rule.delete()
			}
		}
	}
}
