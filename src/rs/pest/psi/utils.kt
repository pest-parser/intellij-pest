package rs.pest.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

val PsiElement.childrenWithLeaves: Sequence<PsiElement>
	get() = generateSequence(this.firstChild) { it.nextSibling }

val PsiElement.startOffset: Int
	get() = textRange.startOffset

val PsiElement.endOffset: Int
	get() = textRange.endOffset

val PsiElement.endOffsetInParent: Int
	get() = startOffsetInParent + textLength

fun PsiElement.rangeWithPrevSpace(prev: PsiElement?) = when (prev) {
	is PsiWhiteSpace -> textRange.union(prev.textRange)
	else -> textRange
}

val PsiElement.rangeWithPrevSpace: TextRange
	get() = rangeWithPrevSpace(prevSibling)

private fun PsiElement.getLineCount(): Int {
	val doc = containingFile?.let { file -> PsiDocumentManager.getInstance(project).getDocument(file) }
	if (doc != null) {
		val spaceRange = textRange ?: TextRange.EMPTY_RANGE

		if (spaceRange.endOffset <= doc.textLength) {
			val startLine = doc.getLineNumber(spaceRange.startOffset)
			val endLine = doc.getLineNumber(spaceRange.endOffset)

			return endLine - startLine
		}
	}

	return (text ?: "").count { it == '\n' } + 1
}

fun PsiWhiteSpace.isMultiLine(): Boolean = getLineCount() > 1
