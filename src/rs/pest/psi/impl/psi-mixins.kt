package rs.pest.psi.impl

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import icons.PestIcons
import rs.pest.PestFile
import rs.pest.psi.*

abstract class PestElement(node: ASTNode) : ASTWrapperPsiElement(node) {
	val containingPestFile get() = containingFile as PestFile
}

abstract class PestGrammarRuleMixin(node: ASTNode) : PestElement(node), PestGrammarRule {
	private var refCache: Array<PsiReference>? = null
	private fun refreshCache(myName: String) = collectFrom<PestIdentifier>(containingFile, myName, this).also { refCache = it }
	override fun getReference() = references.firstOrNull()
	override fun getReferences() = refCache ?: name?.let(::refreshCache) ?: emptyArray()
	override fun subtreeChanged() {
		name?.let(::refreshCache)
		typeCache = null
	}

	override fun getNameIdentifier() = firstChild as? PestIdentifier
	override fun getIcon(flags: Int) = PestIcons.PEST
	override fun getName() = nameIdentifier?.text
	override fun setName(newName: String): PsiElement {
		val nameIdentifier = firstChild
		return when (nameIdentifier) {
			is PestIdentifier -> {
				refCache = references.mapNotNull { it.handleElementRename(newName)?.reference }.toTypedArray()
				this@PestGrammarRuleMixin
			}
			is PestBuiltin -> throw IncorrectOperationException("Cannot rename a builtin rule!")
			else -> this@PestGrammarRuleMixin
		}
	}

	fun preview(maxSizeExpected: Int) = expressionList.lastOrNull()?.body(maxSizeExpected)
	private var typeCache: PestRuleType? = null
	val type: PestRuleType
		get() = typeCache ?: when (modifier?.firstChild?.node?.elementType) {
			PestTypes.SILENT_MODIFIER -> PestRuleType.Silent
			PestTypes.ATOMIC_MODIFIER -> PestRuleType.Atomic
			PestTypes.NON_ATOMIC_MODIFIER -> PestRuleType.NonAtomic
			PestTypes.COMPOUND_ATOMIC_MODIFIER -> PestRuleType.CompoundAtomic
			else -> PestRuleType.Simple
		}.also { typeCache = it }
}

abstract class PestIdentifierMixin(node: ASTNode) : PestElement(node), PsiPolyVariantReference, PsiNameIdentifierOwner {
	private val range = TextRange(0, textLength)
	override fun getNameIdentifier() = this
	override fun getName(): String? = text
	override fun setName(newName: String): PsiElement = replace(PestTokenType.fromText(newName, project))

	private var resolveCache = emptyList<ResolveResult>().toMutableList()
	private fun updateCache(): List<ResolveResult> {
		resolveCache.removeAll { it.element?.isValid ?: false }
		if (resolveCache.isNotEmpty()) return resolveCache
		else resolveCache.addAll(allGrammarRules().filter { it.name == text }.map(::PsiElementResolveResult))
		return resolveCache
	}

	override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.run { element }
	override fun multiResolve(incomplete: Boolean): Array<ResolveResult> = updateCache().toTypedArray()
	override fun getReference() = this
	override fun getReferences() = arrayOf(this)
	private fun allGrammarRules(): Collection<PestGrammarRuleMixin> = containingPestFile.rules().values.filter { it.nameIdentifier != null }
	override fun handleElementRename(newName: String): PsiElement = setName(newName)
	override fun getElement() = this
	override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException("Unsupported")
	override fun isReferenceTo(reference: PsiElement) = reference == resolve()
	override fun getCanonicalText(): String = text
	override fun isSoft() = true
	override fun getRangeInElement() = range
	override fun getVariants() = allGrammarRules().map {
		LookupElementBuilder
			.create(it)
			.withTailText(it.preview(35), true)
			.withIcon(it.getIcon(0))
			.withTypeText(it.type.description)
	}.toTypedArray()
}
