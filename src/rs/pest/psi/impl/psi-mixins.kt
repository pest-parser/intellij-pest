package rs.pest.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import rs.pest.psi.PestGrammarRule
import rs.pest.psi.PestIdentifier

abstract class PestGrammarRuleMixin(node: ASTNode) : ASTWrapperPsiElement(node), PestGrammarRule {
	override fun getNameIdentifier() = firstChild as? PestIdentifier
	override fun getName() = nameIdentifier?.text
	/// TODO
	override fun setName(newName: String): PsiElement = throw IncorrectOperationException("Unsupported")
}

abstract class PestIdentifierMixin(node: ASTNode) : ASTWrapperPsiElement(node), PestIdentifier, PsiPolyVariantReference {
	private val range = TextRange(0, textLength)
	override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.run { element }
	override fun multiResolve(incomplete: Boolean): Array<ResolveResult> = allGrammarRules().filter { it.name == text }.map(::PsiElementResolveResult).toTypedArray()
	override fun getVariants() = allGrammarRules().toTypedArray()
	private fun allGrammarRules(): Collection<PestGrammarRule> = PsiTreeUtil.findChildrenOfType(containingFile, PestGrammarRule::class.java).filter { it.nameIdentifier != null }
	override fun getReference() = this
	override fun getReferences() = arrayOf(reference)
	override fun handleElementRename(newName: String): PsiElement = throw IncorrectOperationException("Unsupported")
	override fun getElement() = this
	override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException("Unsupported")
	override fun isReferenceTo(reference: PsiElement) = reference == this
	override fun getCanonicalText() = text
	override fun isSoft() = true
	override fun getRangeInElement() = range
}
