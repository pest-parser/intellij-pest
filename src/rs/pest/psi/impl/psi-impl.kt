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
	.let { if (self != null) it.filter { it.isReferenceTo(self) } else it }
	.toTypedArray()
