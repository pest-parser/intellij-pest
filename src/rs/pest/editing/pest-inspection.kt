package rs.pest.editing

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import org.intellij.lang.annotations.Language
import rs.pest.PestBundle
import rs.pest.PestFile
import rs.pest.psi.impl.PestGrammarRuleMixin

class DuplicateRuleInspection : LocalInspectionTool() {
	override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
		if (file !is PestFile) return emptyArray()
		val problemsHolder = ProblemsHolder(manager, file, isOnTheFly)
		val ruleSet = hashMapOf<String, PestGrammarRuleMixin>()
		val problematicRules = hashSetOf<PestGrammarRuleMixin>()
		file.rules().forEach {
			val name = it.name
			val tried = ruleSet[name]
			if (tried != null) {
				problematicRules += it
				problematicRules += tried
			} else ruleSet[name] = it
		}
		problematicRules.forEach {
			problemsHolder.registerProblem(it, PestBundle.message("pest.inspection.duplicate-rule", it.name))
		}
		return problemsHolder.resultsArray
	}
}

@Language("RegExp")
private val errorMsgRegex = Regex("\\A(\\d+)\\^(\\d+)\\^(\\d+)\\^(\\d+)\\^(.*)$")

private fun vmListener(element: PestFile) = object : DocumentListener {
	override fun documentChanged(event: DocumentEvent) = reloadVM(event.document, element)
}

fun reloadVM(dom: Document, element: PestFile) {
	if (PsiTreeUtil.hasErrorElements(element)) return
	val (works, messages) = try {
		element.reloadVM(dom.text)
	} catch (e: Exception) {
		element.rebootVM()
		element.reloadVM(dom.text)
	}
	if (works) with(element) {
		errors = emptySequence()
		availableRules = messages
		livePreviewFile().forEach(DaemonCodeAnalyzer.getInstance(element.project)::restart)
	} else with(element) {
		errors = messages.mapNotNull { errorMsgRegex.matchEntire(it)?.groupValues }.mapNotNull {
			val startLine = it[1].toInt() - 1
			val startCol = it[2].toInt() - 1
			val endLine = it[3].toInt() - 1
			val endCol = it[4].toInt() - 1
			val range = try {
				TextRange(dom.getLineStartOffset(startLine) + startCol, dom.getLineStartOffset(endLine) + endCol)
			} catch (e: IndexOutOfBoundsException) {
				return@mapNotNull null
			}
			Pair(range, it[5])
		}
		availableRules = emptySequence()
	}
}

class PestVmInspection : LocalInspectionTool() {
	override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
		if (file !is PestFile) return emptyArray()
		val project = file.project
		if (!file.isDocumentListenerAdded) {
			PsiDocumentManager.getInstance(project).getDocument(file)?.apply {
				reloadVM(this, file)
				file.isDocumentListenerAdded = true
				addDocumentListener(vmListener(file))
			}
		}
		return emptyArray()
	}
}
