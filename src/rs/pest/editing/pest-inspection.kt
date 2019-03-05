package rs.pest.editing

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.util.containers.ContainerUtil
import rs.pest.PestBundle
import rs.pest.PestFile
import rs.pest.psi.impl.PestGrammarRuleMixin

class DuplicateRuleInspection : LocalInspectionTool() {
	override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
		if (file !is PestFile) return emptyArray()
		val problemsHolder = ProblemsHolder(manager, file, isOnTheFly)
		val ruleSet = ContainerUtil.newHashMap<String, PestGrammarRuleMixin>()
		val problematicRules = ContainerUtil.newLinkedHashSet<PestGrammarRuleMixin>()
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
