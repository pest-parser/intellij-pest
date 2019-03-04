package rs.pest.psi.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser


/**
 * @param self The declaration itself
 */
inline fun <reified Element : PsiElement> collectFrom(startPoint: PsiElement, name: String, self: PsiElement? = null) = SyntaxTraverser
	.psiTraverser(startPoint)
	.filter { it is Element && it.text == name && it != self }
	.mapNotNull(PsiElement::getReference)
	.toTypedArray()

fun PsiElement.body(maxSizeExpected: Int) = buildString {
	append(' ')
	var child = firstChild
	while (child != null) {
		append(child.text)
		if (length >= maxSizeExpected) break
		child = child.nextSibling
	}
}
