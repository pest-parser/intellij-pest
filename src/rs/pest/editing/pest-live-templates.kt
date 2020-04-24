package rs.pest.editing

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import rs.pest.*
import rs.pest.psi.PestGrammarBody

class PestDefaultContext : TemplateContextType(PEST_DEFAULT_CONTEXT_ID, PEST_LANGUAGE_NAME) {
	override fun isInContext(file: PsiFile, offset: Int) = file.fileType == PestFileType
}

class PestLocalContext : TemplateContextType(PEST_LOCAL_CONTEXT_ID, PEST_LOCAL_CONTEXT_NAME, PestDefaultContext::class.java) {
	override fun isInContext(file: PsiFile, offset: Int) = file.fileType == PestFileType && inRule(file.findElementAt(offset))
	private fun inRule(element: PsiElement?) =
		PsiTreeUtil.findFirstParent(element) { it is PestGrammarBody } != null
}
