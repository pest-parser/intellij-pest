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
import rs.pest.psi.impl.extractSimilar
import rs.pest.psi.impl.findParentExpression
import java.util.*


class PestIntroduceRuleAction : BasePlatformRefactoringAction() {
	init {
		setInjectedContext(true)
	}

	override fun isAvailableInEditorOnly() = true
	override fun isAvailableForFile(file: PsiFile?) = file is PestFile
	override fun isAvailableForLanguage(language: Language?) = language === PestLanguage.INSTANCE
	override fun isEnabledOnElements(elements: Occurrences) = false
	override fun getRefactoringHandler(provider: RefactoringSupportProvider) = PestIntroduceRuleActionHandler()
}

typealias Occurrences = Array<out PsiElement>

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
			@Suppress("InvalidBundleOrProperty")
			CommonRefactoringUtil.showErrorHint(project, editor,
				RefactoringBundle.message("refactoring.introduce.context.error"),
				PestBundle.message("pest.actions.general.title.error"), null)
			return
		}
		if (!selectionModel.hasSelection()) {
			val expressions = ArrayList<PestExpression>()
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
					@Suppress("InvalidBundleOrProperty")
					RefactoringBundle.message("refactoring.introduce.selection.error"),
					PestBundle.message("pest.actions.general.title.error"), null)
				return
			}
			invokeIntroduce(project, editor, file, currentRule, selectedExpression)
		}
	}

	private fun findSelectedExpressionsInRange(parentExpression: PestExpression, range: TextRange): List<PestExpression> {
		if (parentExpression.textRange == range) return listOf(parentExpression)
		val list = ArrayList<PestExpression>()
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
		val occurrence = mutableMapOf<OccurrencesChooser.ReplaceChoice, List<Occurrences>>()
		occurrence[OccurrencesChooser.ReplaceChoice.NO] = listOf(expressions.toTypedArray())
		val allList = SyntaxTraverser.psiTraverser()
			.withRoot(file)
			.filterIsInstance<PestExpression>()
			.mapNotNull { extractSimilar(expr, it) }
			.toList()
		if (allList.size > 1)
			occurrence[OccurrencesChooser.ReplaceChoice.ALL] = allList
		object : OccurrencesChooser<Occurrences>(editor) {
			override fun getOccurrenceRange(occurrence: Occurrences) =
				TextRange(occurrence.first().startOffset, occurrence.last().endOffset)
		}.showChooser(object : Pass<OccurrencesChooser.ReplaceChoice>() {
			override fun pass(choice: OccurrencesChooser.ReplaceChoice?) = WriteCommandAction.runWriteCommandAction(project) {
				@Suppress("NAME_SHADOWING")
				val rule = file.addAfter(rule, currentRule.nextSibling) as PestGrammarRule
				val document = editor.document
				PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
				val all = OccurrencesChooser.ReplaceChoice.ALL
				if (choice === all) occurrence[all]?.let { exprToReplace ->
					replaceUsages(exprToReplace)
				} else occurrence[OccurrencesChooser.ReplaceChoice.NO]?.let { exprToReplace ->
					replaceUsages(exprToReplace)
				}
				val newExpr = PestTokenType.createExpression(name, project)!!
				if (expressions.size == 1) {
					expressions.first().replace(newExpr)
				} else {
					val firstExpr = expressions.first()
					val parent = firstExpr.parent
					parent.addBefore(newExpr, firstExpr)
					parent.deleteChildRange(firstExpr, expressions.last())
				}
				val newRuleStartOffset = currentRule.endOffset + 1
				val popup = PestIntroduceRulePopupImpl(newRuleStartOffset, rule, editor, project, expr)
				editor.caretModel.moveToOffset(newRuleStartOffset)
				PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
				var lastOffset = newRuleStartOffset + rule.textLength
				val fullLength = document.textLength
				if (lastOffset > fullLength) lastOffset = fullLength
				document.insertString(lastOffset, "\n")
				PsiDocumentManager.getInstance(project).commitDocument(document)
				popup.performInplaceRefactoring(null)
			}

			private fun replaceUsages(expressions: List<Occurrences>) {
				val newExpr = PestTokenType.createExpression(name, project)!!
				if (expressions.size == 1) {
					expressions.first().first().replace(newExpr)
				} else expressions.forEach {
					val firstExpr = it.first()
					val parent = firstExpr.parent
					parent.addBefore(newExpr, firstExpr)
					parent.deleteChildRange(firstExpr, it.last())
				}
			}
		}, occurrence)
	}

	// Unsupported
	override fun invoke(project: Project, elements: Occurrences, dataContext: DataContext?) = Unit
}
