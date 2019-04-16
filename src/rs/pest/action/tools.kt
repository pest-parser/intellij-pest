package rs.pest.action

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.util.PsiTreeUtil
import rs.pest.PEST_WEBSITE
import rs.pest.PestFile
import rs.pest.livePreview.livePreview

class PestViewSiteAction : AnAction(), DumbAware {
	override fun actionPerformed(e: AnActionEvent) {
		BrowserLauncher.instance.open(PEST_WEBSITE)
	}
}

class PestLivePreviewAction : AnAction(), DumbAware {
	override fun update(e: AnActionEvent) {
		super.update(e)
		val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext)
		e.presentation.isEnabledAndVisible = psiFile is PestFile && !PsiTreeUtil.hasErrorElements(psiFile)
	}

	override fun actionPerformed(e: AnActionEvent) {
		val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext)
			as? PestFile ?: return
		livePreview(psiFile)
	}
}
