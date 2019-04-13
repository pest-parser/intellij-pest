package rs.pest.action

import com.intellij.lang.Language
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pass
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.IntroduceTargetChooser
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.actions.BasePlatformRefactoringAction
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.containers.ContainerUtil
import rs.pest.PestBundle
import rs.pest.PestFile
import rs.pest.PestLanguage
import rs.pest.action.ui.PestIntroduceRulePopupImpl
import rs.pest.psi.*
import rs.pest.psi.impl.PestGrammarRuleMixin
import rs.pest.psi.impl.bodyText
import rs.pest.psi.impl.compareExpr
import rs.pest.psi.impl.findParentExpression
import java.util.*


class PestIntroduceRuleAction : BasePlatformRefactoringAction() {
	init {
		setInjectedContext(true)
	}

	override fun isAvailableInEditorOnly() = true
	override fun isAvailableForFile(file: PsiFile?) = file is PestFile
	override fun isAvailableForLanguage(language: Language?) = language === PestLanguage.INSTANCE
	override fun isEnabledOnElements(elements: Array<out PsiElement>) = false
	override fun getRefactoringHandler(provider: RefactoringSupportProvider) = PestIntroduceRuleActionHandler()
}

class PestIntroduceRuleActionHandler : RefactoringActionHandler {
	override fun invoke(project: Project, editor: Editor, file: PsiFile?, dataContext: DataContext?) {
		if (file !is PestFile) return
		val selectionModel = editor.selectionModel
		val starts = selectionModel.blockSelectionStarts
		val ends = selectionModel.blockSelectionEnds
		if (starts.isEmpty() || ends.isEmpty()) return

		val startOffset = starts.first()
		val endOffset = ends.last()
		val currentRule = PsiTreeUtil.getParentOfType(file.findElementAt(startOffset), PestGrammarRuleMixin::class.java)
		var parentExpression = if (currentRule != null) findParentExpression<PestExpression>(file, startOffset, endOffset) else null
		if (currentRule == null || parentExpression == null) {
			CommonRefactoringUtil.showErrorHint(project, editor,
				RefactoringBundle.message("refactoring.introduce.context.error"),
				PestBundle.message("pest.actions.general.title.error"), null)
			return
		}
		if (!selectionModel.hasSelection()) {
			val expressions = ContainerUtil.newArrayList<PestExpression>()
			while (parentExpression != null) {
				expressions.add(parentExpression)
				parentExpression = PsiTreeUtil.getParentOfType<PestExpression>(parentExpression, PestExpression::class.java)
			}
			if (expressions.size == 1) {
				invokeIntroduce(project, editor, file, currentRule, expressions)
			} else {
				IntroduceTargetChooser.showChooser(
					editor, expressions,
					object : Pass<PestExpression>() {
						override fun pass(expression: PestExpression) {
							invokeIntroduce(project, editor, file, currentRule, Collections.singletonList(expression))
						}
					}, { it.bodyText(it.textLength) }, PestBundle.message("pest.actions.extract.rule.target-chooser.title")
				)
			}
		} else {
			val selectedExpression = findSelectedExpressionsInRange(parentExpression, TextRange(startOffset, endOffset))
			if (selectedExpression.isEmpty()) {
				CommonRefactoringUtil.showErrorHint(project, editor,
					RefactoringBundle.message("refactoring.introduce.selection.error"),
					PestBundle.message("pest.actions.general.title.error"), null)
				return
			}
			invokeIntroduce(project, editor, file, currentRule, selectedExpression)
		}
	}

	private fun findSelectedExpressionsInRange(parentExpression: PestExpression, range: TextRange): List<PestExpression> {
		if (parentExpression.textRange == range) return listOf(parentExpression)
		val list = ContainerUtil.newArrayList<PestExpression>()
		var c: PsiElement? = parentExpression.firstChild
		while (c != null) {
			if (c is PsiWhiteSpace) {
				c = c.nextSibling
				continue
			}
			if (c.textRange.intersectsStrict(range)) {
				if (c is PestExpression) list.add(c)
				else if (c === parentExpression.firstChild || c === parentExpression.lastChild)
					return Collections.singletonList(parentExpression)
			}
			c = c.nextSibling
		}
		return list
	}

	private fun invokeIntroduce(project: Project, editor: Editor, file: PestFile, currentRule: PestGrammarRuleMixin, expressions: List<PestExpression>) {
		val first = expressions.first()
		val last = expressions.last()
		val currentRuleName = currentRule.name
		var i = 0
		var name: String
		while (true) {
			name = "$currentRuleName$i"
			if (file.rules().any { it.name == name }) i++
			else break
		}
		val range = TextRange(first.startOffset, last.endOffset)
		val rule = PestTokenType.createRule("$name = { ${range.shiftLeft(currentRule.startOffset).substring(currentRule.text).trim()} }", project)!!
		val expr = rule.grammarBody!!.expression!!
		val occurrence = mutableMapOf<OccurrencesChooser.ReplaceChoice, MutableList<Array<PestExpression>>>()
		occurrence[OccurrencesChooser.ReplaceChoice.NO] = mutableListOf(expressions.toTypedArray())
		val allList = mutableListOf<Array<PestExpression>>()
		file.acceptChildren(object: PestVisitor() {
			override fun visitExpression(o: PestExpression) {
				if (compareExpr(expr, o)) allList.add(arrayOf(o))
				super.visitExpression(o)
			}
		})
		if (allList.size > 1)
			occurrence[OccurrencesChooser.ReplaceChoice.ALL] = allList
		object : OccurrencesChooser<Array<PestExpression>>(editor) {
			override fun getOccurrenceRange(occurrence: Array<PestExpression>) =
				TextRange(occurrence.first().startOffset, occurrence.last().endOffset)
		}.showChooser(object : Pass<OccurrencesChooser.ReplaceChoice>() {
			override fun pass(t: OccurrencesChooser.ReplaceChoice?) {
				WriteCommandAction.runWriteCommandAction(project) {
					file.addAfter(rule, currentRule.nextSibling)
					val document = editor.document
					PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
					if (expressions.size == 1) {
						expressions.first().replace(PestTokenType.createExpression(name, project)!!)
					} else {
						document.deleteString(range.startOffset, range.endOffset)
						document.insertString(range.startOffset, name)
					}
					val popup = PestIntroduceRulePopupImpl(rule, editor, project, expr)
					val newRuleStartOffset = currentRule.endOffset + 1
					editor.caretModel.moveToOffset(newRuleStartOffset)
					PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
					var lastOffset = newRuleStartOffset + rule.textLength
					val fullLength = document.textLength
					if (lastOffset > fullLength) lastOffset = fullLength
					document.insertString(lastOffset, "\n")
					popup.performInplaceRefactoring(null)
				}
			}
		}, occurrence)
	}

	// Unsupported
	override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) = Unit
}
