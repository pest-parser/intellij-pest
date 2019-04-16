package rs.pest.action

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.util.PsiTreeUtil
import rs.pest.PEST_WEBSITE
import rs.pest.PestBundle
import rs.pest.PestFile
import rs.pest.action.ui.RuleSelector
import rs.pest.livePreview.livePreview

class PestViewSiteAction : AnAction(), DumbAware {
	override fun actionPerformed(e: AnActionEvent) {
		BrowserLauncher.instance.open(PEST_WEBSITE)
	}
}

class PestLivePreviewAction : AnAction(), DumbAware {
	override fun update(e: AnActionEvent) {
		super.update(e)
		val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext) as? PestFile ?: return
		e.presentation.isEnabledAndVisible =
			CommonDataKeys.EDITOR.getData(e.dataContext) != null
				&& !PsiTreeUtil.hasErrorElements(psiFile)
				&& psiFile.errors.none()
	}

	override fun actionPerformed(e: AnActionEvent) {
		val file = CommonDataKeys.PSI_FILE.getData(e.dataContext) as? PestFile ?: return
		val parentComponent = FileEditorManagerEx.getInstanceEx(file.project)
			.getEditors(file.virtualFile)
			.firstOrNull()
			?.component
			?: return
		val popup = RuleSelector().apply {
			file.availableRules.forEach(ruleCombo::addItem)
			ruleCombo.selectedIndex = 0
			okButton.addActionListener {
				val selected = ruleCombo.selectedItem.toString()
				livePreview(file, selected)
			}
		}
		JBPopupFactory.getInstance()
			.createDialogBalloonBuilder(popup.mainPanel, PestBundle.message("pest.actions.live-preview.popup.title"))
			.createBalloon()
			.showInCenterOf(parentComponent)
	}
}
