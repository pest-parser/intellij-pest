package rs.pest.psi.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SyntaxTraverser


/**
 * @param self The declaration itself
 */
inline fun <reified Element : PsiElement> collectFrom(startPoint: PsiElement, name: String, self: PsiElement? = null) = SyntaxTraverser
	.psiTraverser(startPoint)
	.filter { it is Element && it.text == name && it != self }
	.mapNotNull(PsiElement::getReference)
	.toTypedArray()

fun PsiElement.bodyText(maxSizeExpected: Int) = buildString {
	append(' ')
	var child = firstChild
	while (child != null && child != this@bodyText) {
		if (child is PsiWhiteSpace) append(' ')
		else {
			while (child.firstChild != null && length + child.textLength > maxSizeExpected) child = child.firstChild
			append(child.text)
		}
		if (length >= maxSizeExpected) break
		do {
			child = child.nextSibling ?: child.parent
		} while (child == null)
	}
}
