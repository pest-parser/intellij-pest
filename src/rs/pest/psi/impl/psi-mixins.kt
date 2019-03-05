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
	val containingPestFile get() = containingFile as? PestFile
	protected fun allGrammarRules(): Collection<PestGrammarRuleMixin> = containingPestFile?.rules().orEmpty()
}

fun renameBuiltin(): Nothing = throw IncorrectOperationException("Cannot rename a builtin rule!")

abstract class PestGrammarRuleMixin(node: ASTNode) : PestElement(node), PestGrammarRule {
	private var refCache: Array<PsiReference>? = null
	private fun refreshCache(myName: String) = collectFrom<PestIdentifier>(containingFile, myName, this).also { refCache = it }
	override fun getReference() = references.firstOrNull()
	override fun getReferences() = refCache ?: refreshCache(name) ?: emptyArray()
	override fun subtreeChanged() {
		refreshCache(name)
		typeCache = null
	}

	override fun getNameIdentifier(): PsiElement = firstChild
	override fun getIcon(flags: Int) = PestIcons.PEST
	override fun getName(): String = nameIdentifier.text
	override fun setName(newName: String) = when (nameIdentifier) {
		is PestIdentifier -> {
			refCache = references.mapNotNull { it.handleElementRename(newName)?.reference }.toTypedArray()
			this@PestGrammarRuleMixin
		}
		is PestBuiltin -> renameBuiltin()
		else -> this@PestGrammarRuleMixin
	}


	fun preview(maxSizeExpected: Int) = grammarBody?.bodyText(maxSizeExpected)
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

abstract class PestResolvableMixin(node: ASTNode) : PestExpressionImpl(node), PsiPolyVariantReference {
	private val range = TextRange(0, textLength)
	override fun isSoft() = true
	override fun getRangeInElement() = range

	private var resolveCache = emptyList<ResolveResult>().toMutableList()
	private fun updateCache(): List<ResolveResult> {
		resolveCache.removeAll { it.element?.isValid ?: false }
		if (resolveCache.isNotEmpty()) return resolveCache
		else resolveCache.addAll(allGrammarRules().filter { it.name == text }.map(::PsiElementResolveResult))
		return resolveCache
	}

	override fun getReference() = this
	override fun getReferences() = arrayOf(reference)
	override fun isReferenceTo(reference: PsiElement) = reference == resolve()
	override fun getCanonicalText(): String = text
	override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.run { element }
	override fun multiResolve(incomplete: Boolean): Array<ResolveResult> = updateCache().toTypedArray()
	override fun getElement() = this
	override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException("Unsupported")
	override fun getVariants() = allGrammarRules().map {
		LookupElementBuilder
			.create(it)
			.withTailText(it.preview(35), true)
			.withIcon(it.getIcon(0))
			.withTypeText(it.type.description)
	}.toTypedArray()
}

abstract class PestIdentifierMixin(node: ASTNode) : PestResolvableMixin(node), PsiNameIdentifierOwner {
	override fun getNameIdentifier() = this
	override fun getName(): String? = text
	override fun setName(newName: String): PsiElement = replace(PestTokenType.fromText(newName, project))

	override fun handleElementRename(newName: String): PsiElement = setName(newName)
}

abstract class PestBuiltinMixin(node: ASTNode) : PestResolvableMixin(node) {
	override fun handleElementRename(newName: String): PsiElement = renameBuiltin()
}

abstract class PestStringMixin(node: ASTNode) : PestExpressionImpl(node), PsiLanguageInjectionHost {
	override fun isValidHost() = true
	override fun updateText(text: String) = replace(PestTokenType.fromText(text, project)) as? PestStringMixin
	override fun createLiteralTextEscaper(): LiteralTextEscaper<PestStringMixin> = PestStringEscaper(this)
}
