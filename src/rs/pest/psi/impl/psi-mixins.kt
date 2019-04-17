package rs.pest.psi.impl

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.util.IncorrectOperationException
import icons.PestIcons
import rs.pest.PestFile
import rs.pest.psi.*

abstract class PestElement(node: ASTNode) : ASTWrapperPsiElement(node) {
	val containingPestFile get() = containingFile as? PestFile
	protected fun allGrammarRules(): Collection<PestGrammarRuleMixin> = containingPestFile?.rules().orEmpty()
	override fun getUseScope(): LocalSearchScope {
		val containingFile = containingFile
		return LocalSearchScope(containingFile, containingFile.name)
	}
}

abstract class PestGrammarRuleMixin(node: ASTNode) : PestElement(node), PsiNameIdentifierOwner, PestGrammarRule {
	private var recCache: Boolean? = null
	val isRecursive
		get() = recCache ?: run {
			val grammarBody = grammarBody?.expression ?: return@run false
			val name = name
			SyntaxTraverser
				.psiTraverser(grammarBody)
				.filterTypes { it == PestTypes.IDENTIFIER }
				.any { it.text == name }
		}.also { recCache = it }

	fun refreshReferenceCache() = refreshReferenceCache(name, nameIdentifier)
	private fun refreshReferenceCache(myName: String, self: PsiElement) = collectFrom<PestIdentifier>(containingFile, myName, self)
	override fun subtreeChanged() {
		typeCache = null
		recCache = null
	}

	val docComment: PsiComment? get() = firstChild as? PsiComment
	override fun getNameIdentifier(): PsiElement = ruleName.firstChild
	override fun getIcon(flags: Int) = PestIcons.PEST
	override fun getName(): String = nameIdentifier.text
	@Throws(IncorrectOperationException::class)
	override fun setName(newName: String): PsiElement {
		firstChild.replace(PestTokenType.createRuleName(newName, project)
			?: throw IncorrectOperationException("Invalid name $newName"))
		return this
	}


	fun preview(maxSizeExpected: Int) = grammarBody?.expression?.bodyText(maxSizeExpected)
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
	override fun isSoft() = true
	override fun getRangeInElement() = TextRange(0, textLength)

	override fun getReference() = this
	override fun getReferences() = arrayOf(reference)
	override fun isReferenceTo(reference: PsiElement) = reference == resolve()
	override fun getCanonicalText(): String = text
	override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.run { element }
	override fun multiResolve(incomplete: Boolean): Array<ResolveResult> = allGrammarRules().filter { it.name == text }.map(::PsiElementResolveResult).toTypedArray()
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


abstract class PestIdentifierMixin(node: ASTNode) : PestResolvableMixin(node) {
	@Throws(IncorrectOperationException::class)
	override fun handleElementRename(newName: String): PsiElement = replace(
		PestTokenType.createExpression(newName, project)
			?: throw IncorrectOperationException("Invalid name: $newName"))
}

abstract class PestBuiltinMixin(node: ASTNode) : PestResolvableMixin(node) {
	@Throws(IncorrectOperationException::class)
	override fun handleElementRename(newName: String): PsiElement = replace(
		PestTokenType.createExpression(newName, project)
			?: throw IncorrectOperationException("Invalid name: $newName"))
}

abstract class PestStringMixin(node: ASTNode) : PestExpressionImpl(node), PsiLanguageInjectionHost {
	override fun isValidHost() = true
	override fun updateText(text: String) = PestTokenType.createExpression(text, project)?.let(::replace) as? PestStringMixin
	override fun createLiteralTextEscaper(): LiteralTextEscaper<PestStringMixin> = PestStringEscaper(this)
}

abstract class PestFixedBuiltinRuleNameMixin(node: ASTNode) : PestElement(node), PestFixedBuiltinRuleName
abstract class PestCustomizableRuleNameMixin(node: ASTNode) : PestResolvableMixin(node), PestCustomizableRuleName {
	@Throws(IncorrectOperationException::class)
	override fun handleElementRename(newName: String): PsiElement = throw IncorrectOperationException("Cannot rename")
}

abstract class PestRuleNameMixin(node: ASTNode) : PestElement(node), PestValidRuleName
