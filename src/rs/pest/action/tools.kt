package rs.pest.action

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import rs.pest.PEST_WEBSITE

class PestViewSiteAction : AnAction(), DumbAware {
	override fun actionPerformed(e: AnActionEvent) {
		BrowserLauncher.instance.open(PEST_WEBSITE)
	}
}
