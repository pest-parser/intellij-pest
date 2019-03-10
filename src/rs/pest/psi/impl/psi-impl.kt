package rs.pest.psi.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.util.PsiTreeUtil


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
	var endOffset = endOffset
	if (endOffset > startOffset) endOffset--
	val startElement = file.findElementAt(startOffset)
	val endElement = file.findElementAt(endOffset)
	if (startElement == null || endElement == null) return null
	val commonParent = PsiTreeUtil.findCommonParent(startElement, endElement)
	return PsiTreeUtil.getParentOfType(commonParent, T::class.java, false)
}
