package rs.pest.psi.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.util.PsiTreeUtil
import rs.pest.psi.*


/**
 * @param self The declaration itself
 */
inline fun <reified Element : PsiElement> collectFrom(startPoint: PsiElement, name: String, self: PsiElement? = null) = SyntaxTraverser
	.psiTraverser(startPoint)
	.filterIsInstance<Element>()
	.filter { it.text == name && it != self }
	.mapNotNull(PsiElement::getReference)
	.toMutableList()

fun PsiElement.bodyText(maxSizeExpected: Int) = buildString {
	append(' ')
	var child = firstChild
	while (child != null && child != this@bodyText) {
		if (child is PsiWhiteSpace) append(' ')
		else {
			while (child.firstChild != null && length + child.textLength > maxSizeExpected) child = child.firstChild
			append(child.text)
		}
		if (maxSizeExpected < 0 || length >= maxSizeExpected) break
		do {
			child = child.nextSibling ?: child.parent
		} while (child == null)
	}
}

inline fun <reified T : PsiElement> findParentExpression(file: PsiFile, startOffset: Int, endOffset: Int): T? {
	@Suppress("NAME_SHADOWING")
	var endOffset = endOffset
	if (endOffset > startOffset) endOffset--
	val startElement = file.findElementAt(startOffset)
	val endElement = file.findElementAt(endOffset)
	if (startElement == null || endElement == null) return null
	val commonParent = PsiTreeUtil.findCommonParent(startElement, endElement)
	return PsiTreeUtil.getParentOfType(commonParent, T::class.java, false)
}

fun compareExpr(l: PsiElement, r: PsiElement): Boolean {
	if (l === r) return true
	if (!l.isValid || !r.isValid) return false
	return when {
		l is PestCustomizableRuleName && r is PestCustomizableRuleName
			|| l is PestBuiltin && r is PestBuiltin
			|| l is PestString && r is PestString
			|| l is PestInfixOperator && r is PestInfixOperator
			|| l is PestPrefixOperator && r is PestPrefixOperator
			|| l is PestIdentifier && r is PestIdentifier
			|| l is PestPush && r is PestPush
			|| l is PestPeek && r is PestPeek
			|| l is PestRange && r is PestRange
			|| l is PestPeekSlice && r is PestPeekSlice
			|| l is PestCharacter && r is PestCharacter -> l.textMatches(r)
		l.javaClass == r.javaClass && r.javaClass == PestExpressionImpl::class.java
			|| l is PestPostfixOperator && r is PestPostfixOperator -> {
			val lc = l.children
			val rc = r.children
			lc.size == rc.size
				&& lc.isNotEmpty()
				&& (lc zip rc).all { (x, y) -> compareExpr(x, y) }
		}
		else -> false
	}
}
