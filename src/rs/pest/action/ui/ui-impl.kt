package rs.pest.action.ui

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.parentOfType
import org.rust.lang.core.psi.ext.startOffset
import rs.pest.psi.PestExpression
import rs.pest.psi.PestGrammarBody
import javax.swing.ButtonGroup

class PestIntroduceRulePopupImpl(
	elementToRename: PsiNamedElement,
	editor: Editor,
	project: Project,
	expr: PestExpression
) : PestIntroduceRulePopup(elementToRename, editor, project, expr) {
	init {
		mainPanel.border = null
		val buttons = listOf(atomic, compoundAtomic, nonAtomic, normal, silent)
		with(ButtonGroup()) { buttons.forEach(::add) }
		buttons.forEach { button ->
			button.addChangeListener {
				val runnable = act@{
					val document = myEditor.document
					val grammarBody = expr.parentOfType<PestGrammarBody>() ?: return@act
					val offset = grammarBody.startOffset - 1
					when (button) {
						normal -> document.deleteString(offset, offset + 1)
						atomic -> document.insertString(offset, "@")
						nonAtomic -> document.insertString(offset, "!")
						compoundAtomic -> document.insertString(offset, "$")
						silent -> document.insertString(offset, "_")
					}
					PsiDocumentManager.getInstance(myProject).commitDocument(document)
				}
				val lookup = LookupManager.getActiveLookup(myEditor) as? LookupImpl
				if (lookup != null) {
					lookup.performGuardedChange(runnable)
				} else {
					runnable()
				}
			}
		}
	}

	override fun getComponent() = mainPanel
}
