package rs.pest.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import rs.pest.psi.PestBuiltin
import rs.pest.psi.PestGrammarRule
import rs.pest.psi.PestIdentifier
import rs.pest.psi.PestTokenType

abstract class PestGrammarRuleMixin(node: ASTNode) : ASTWrapperPsiElement(node), PestGrammarRule {
	var cache: Array<PsiReference>? = null
	private fun refreshCache(myName: String) = collectFrom<PestIdentifier>(containingFile, myName, this).also { cache = it }
	override fun getReference() = references.firstOrNull()
	override fun getReferences() = cache ?: name?.let(::refreshCache) ?: emptyArray()
	override fun subtreeChanged() {
		name?.let(::refreshCache)
	}

	override fun getNameIdentifier() = firstChild as? PestIdentifier
	override fun getName() = nameIdentifier?.text
	override fun setName(newName: String): PsiElement {
		val nameIdentifier = firstChild
		return when (nameIdentifier) {
			is PestIdentifier -> {
				nameIdentifier.replace(PestTokenType.fromText(newName, project))
				this@PestGrammarRuleMixin
			}
			is PestBuiltin -> throw IncorrectOperationException("Cannot rename a builtin rule!")
			else -> this@PestGrammarRuleMixin
		}
	}
}

abstract class PestIdentifierMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiPolyVariantReference, PsiNameIdentifierOwner {
	private val range = TextRange(0, textLength)
	override fun getNameIdentifier() = this
	override fun getName() = text
	override fun setName(newName: String): PsiElement = replace(PestTokenType.fromText(newName, project))

	override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.run { element }
	override fun multiResolve(incomplete: Boolean): Array<ResolveResult> = allGrammarRules().filter { it.name == text }.map(::PsiElementResolveResult).toTypedArray()
	override fun getVariants() = allGrammarRules().toTypedArray()
	override fun getReference() = this
	override fun getReferences() = arrayOf(this)
	private fun allGrammarRules(): Collection<PestGrammarRule> = PsiTreeUtil.findChildrenOfType(containingFile, PestGrammarRule::class.java).filter { it.nameIdentifier != null }
	override fun handleElementRename(newName: String): PsiElement = setName(newName)
	override fun getElement() = this
	override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException("Unsupported")
	override fun isReferenceTo(reference: PsiElement) = reference == resolve()
	override fun getCanonicalText() = text
	override fun isSoft() = true
	override fun getRangeInElement() = range
}
