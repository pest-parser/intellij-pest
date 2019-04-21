package rs.pest.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.util.PsiTreeUtil
import rs.pest.PestBundle
import rs.pest.PestFile
import rs.pest.action.ui.PestIdeBridgeInfoImpl
import rs.pest.action.ui.RuleSelector
import rs.pest.livePreview.livePreview

class PestViewInfoAction : AnAction(), DumbAware {
	override fun actionPerformed(e: AnActionEvent) {
		val editor = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return
		val component = PestIdeBridgeInfoImpl().component
		JBPopupFactory.getInstance()
			.createDialogBalloonBuilder(component, PestBundle.message("pest.actions.info.title"))
			.setHideOnClickOutside(true)
			.createBalloon()
			.showInCenterOf(editor.component)
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
				&& psiFile.availableRules.any()
	}

	override fun actionPerformed(e: AnActionEvent) {
		val file = CommonDataKeys.PSI_FILE.getData(e.dataContext) as? PestFile ?: return
		val parentComponent = FileEditorManagerEx.getInstanceEx(file.project)
			.getEditors(file.virtualFile)
			.firstOrNull()
			?.component
			?: return
		lateinit var balloon: Balloon
		val popup = RuleSelector().apply {
			file.availableRules.map { object { override fun toString() = it } }.forEach(ruleCombo::addItem)
			ruleCombo.selectedIndex = 0
			okButton.addActionListener {
				balloon.hide(true)
				val selected = ruleCombo.selectedItem.toString()
				livePreview(file, selected)
			}
		}
		balloon = JBPopupFactory.getInstance()
			.createDialogBalloonBuilder(popup.mainPanel, PestBundle.message("pest.actions.live-preview.popup.title"))
			.setHideOnClickOutside(true)
			.createBalloon()
		balloon.showInCenterOf(parentComponent)
	}
}
