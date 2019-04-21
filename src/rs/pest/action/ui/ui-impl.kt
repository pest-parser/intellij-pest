package rs.pest.action.ui

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import rs.pest.PEST_IDE_CRATE_LINK
import rs.pest.PEST_WEBSITE
import rs.pest.livePreview.Lib
import rs.pest.psi.PestExpression
import rs.pest.psi.PestGrammarRule
import rs.pest.psi.startOffset
import rs.pest.vm.PestUtil
import javax.swing.ButtonGroup

@Suppress("unused")
class PestIntroduceRulePopupImpl(
	newRuleStartOffset: Int,
	elementToRename: PestGrammarRule,
	editor: Editor,
	project: Project,
	expr: PestExpression
) : PestIntroduceRulePopup(elementToRename, editor, project, expr) {
	init {
		mainPanel.border = null
		val buttons = listOf(atomic, compoundAtomic, nonAtomic, normal, silent)
		with(ButtonGroup()) { buttons.forEach(::add) }
		buttons.forEach { button ->
			button.addActionListener {
				val runnable = act@{
					val document = myEditor.document
					val grammarBody = elementToRename.grammarBody ?: return@act
					val local = elementToRename.startOffset == 0
					var offset = if (local) newRuleStartOffset + grammarBody.startOffset
					else grammarBody.startOffset - 1
					if (document.immutableCharSequence[offset] in "@!_$")
						document.deleteString(offset, offset + 1)
					else offset += 1
					when (button) {
						normal -> Unit
						atomic -> document.insertString(offset, "@")
						nonAtomic -> document.insertString(offset, "!")
						compoundAtomic -> document.insertString(offset, "$")
						silent -> document.insertString(offset, "_")
					}
					PsiDocumentManager.getInstance(myProject).commitDocument(document)
				}
				WriteCommandAction.runWriteCommandAction(project) {
					val lookup = LookupManager.getActiveLookup(myEditor) as? LookupImpl
					if (lookup != null) lookup.performGuardedChange(runnable)
					else runnable()
				}
			}
		}
	}

	override fun getComponent() = mainPanel
}

class PestIdeBridgeInfoImpl : PestIdeBridgeInfo() {
	val component get() = mainPanel

	companion object {
		val info by lazy { Lib(PestUtil(114514 * 50)).crateInfo() }
	}

	init {
		websiteLink.text = PEST_WEBSITE
		websiteLink.setListener({ _, _ -> BrowserLauncher.instance.open(PEST_WEBSITE) }, null)
		crateLink.text = PEST_IDE_CRATE_LINK
		crateLink.setListener({ _, _ -> BrowserLauncher.instance.open(PEST_IDE_CRATE_LINK) }, null)
		versionLabel.text = info.version
		authorLabel.text = info.author
		descriptionLabel.text = info.description
	}
}
