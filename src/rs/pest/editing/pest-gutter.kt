package rs.pest.editing

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.util.FunctionUtil
import rs.pest.psi.impl.PestGrammarRuleMixin

class PestRecursionLineMarkerProvider : LineMarkerProvider {
	override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null
	override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<*>>) {
		elements
			.asSequence()
			.filterIsInstance<PestGrammarRuleMixin>()
			.filter(PestGrammarRuleMixin::isRecursive)
			.mapTo(result, ::RecMarkerInfo)
	}
}

private class RecMarkerInfo internal constructor(id: PsiElement) : LineMarkerInfo<PsiElement>(
	id, id.textRange, AllIcons.Gutter.RecursiveMethod, FunctionUtil.constant("Recursive rule"), null, GutterIconRenderer.Alignment.RIGHT) {
	override fun createGutterRenderer(): LineMarkerGutterIconRenderer<PsiElement>? =
		if (myIcon == null) null else object : LineMarkerGutterIconRenderer<PsiElement>(this) {
			override fun getClickAction(): AnAction? = null
		}
}
