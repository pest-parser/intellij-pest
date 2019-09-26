package rs.pest.action

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.PsiTreeUtil
import rs.pest.PestBundle
import rs.pest.PestFile
import rs.pest.action.ui.PestIdeBridgeInfoImpl
import rs.pest.action.ui.RuleSelector
import rs.pest.livePreview.LivePreviewFile
import rs.pest.livePreview.defaultPreviewToHtml
import rs.pest.livePreview.livePreview

class PestViewInfoAction : AnAction(), DumbAware {
	override fun actionPerformed(e: AnActionEvent) {
		val component = PestIdeBridgeInfoImpl().component
		DialogBuilder()
			.title(PestBundle.message("pest.actions.info.title"))
			.centerPanel(component)
			.show()
	}
}

class PestLivePreviewToHtmlAction : AnAction() {
	override fun update(e: AnActionEvent) {
		super.update(e)
		e.presentation.isEnabledAndVisible = CommonDataKeys.PSI_FILE.getData(e.dataContext) is LivePreviewFile
	}

	override fun actionPerformed(e: AnActionEvent) {
		val file = CommonDataKeys.PSI_FILE.getData(e.dataContext) as? LivePreviewFile ?: return
		ProgressManager.getInstance().run(object : Task.Backgroundable(file.project, "HTML Generation", true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
			override fun run(indicator: ProgressIndicator) {
				val startTime = System.currentTimeMillis()
				defaultPreviewToHtml(file, indicator)
				val s = "HTML file generated in ${StringUtil.formatDuration(System.currentTimeMillis() - startTime)}"
				Notifications.Bus.notify(Notification("HTML Export", "", s, NotificationType.INFORMATION))
			}
		})
	}
}

class PestLivePreviewAction : AnAction() {
	override fun update(e: AnActionEvent) {
		super.update(e)
		val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext) as? PestFile
		if (psiFile == null) {
			e.presentation.isEnabledAndVisible = false
			return
		}
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
			file.availableRules.map {
				object {
					override fun toString() = it
				}
			}.forEach(ruleCombo::addItem)
			ruleCombo.selectedIndex = 0
			okButton.addActionListener {
				balloon.hide(true)
				val selectedItem: Any? = ruleCombo.selectedItem
				livePreview(file, selectedItem.toString())
			}
		}
		balloon = JBPopupFactory.getInstance()
			.createDialogBalloonBuilder(popup.mainPanel, PestBundle.message("pest.actions.live-preview.popup.title"))
			.setHideOnClickOutside(true)
			.createBalloon()
		balloon.showInCenterOf(parentComponent)
	}
}