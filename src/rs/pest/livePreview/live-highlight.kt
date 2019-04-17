package rs.pest.livePreview

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import java.awt.Color

fun rgbToAttributes(rgb: String) = Color::class.java.fields
	.firstOrNull { it.name.equals(rgb, ignoreCase = true) }
	?.let { it.get(null) as? Color }
	?.let { TextAttributes().apply { foregroundColor = it } }

@Language("RegExp")
private val lexicalRegex = Regex("\\A(\\d+)\\^(\\d+)\\^(.*)$")

class LivePreviewAnnotator : Annotator {
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		if (element !is LivePreviewFile) return
		val pestFile = element.pestFile ?: return
		val ruleName = element.ruleName ?: return
		val rules = pestFile.rules().mapNotNull { it.docComment?.let { doc -> it.name to doc } }.toMap()
		val vm = pestFile.vm
		when (val res = try {
			vm.renderCode(ruleName, element.text)
		} catch (e: IndexOutOfBoundsException) {
			vm.reboot()
			vm.loadVM(pestFile.text)
			vm.renderCode(ruleName, element.text)
		}) {
			is Rendering.Err -> {
				// TODO error handling
			}
			is Rendering.Ok -> {
				res.lexical.mapNotNull { lexicalRegex.matchEntire(it) }.forEach {
					val (_, start, end, rule) = it.groupValues
					val docComment = rules[rule] ?: return@forEach
					val attr = docComment.text.removePrefix("///").trim().let(::rgbToAttributes) ?: return@forEach
					val range = TextRange(start.toInt(), end.toInt())
					holder.createInfoAnnotation(range, null).enforcedTextAttributes = attr
				}
			}
		}
	}
}
