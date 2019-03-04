package rs.pest.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import rs.pest.psi.impl.PestStringMixin

class PestStringManipulator : AbstractElementManipulator<PestStringMixin>() {
	override fun getRangeInElement(element: PestStringMixin) = getStringTokenRange(element)
	override fun handleContentChange(psi: PestStringMixin, range: TextRange, newContent: String): PestStringMixin? {
		val oldText = psi.text
		val newText = oldText.substring(0, range.startOffset) + newContent + oldText.substring(range.endOffset)
		return psi.updateText(newText)
	}

	companion object {
		fun getStringTokenRange(element: PestStringMixin) = TextRange.from(1, element.textLength - 2)
	}
}