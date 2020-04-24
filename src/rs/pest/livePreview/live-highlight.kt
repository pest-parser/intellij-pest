package rs.pest.livePreview

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import org.intellij.lang.annotations.Language
import rs.pest.PestBundle
import rs.pest.psi.PestTokenType
import rs.pest.psi.childrenWithLeaves
import rs.pest.psi.elementType
import java.awt.Color
import javax.swing.JPanel

fun rgbToAttributes(rgb: String) = stringToColor(rgb)
	?.let { TextAttributes().apply { foregroundColor = it } }

private fun stringToColor(rgb: String) = when {
	rgb.startsWith("#") -> rgb.drop(1).toIntOrNull(16)?.let(::Color)
	else -> Color::class.java.fields
		.firstOrNull { it.name.equals(rgb, ignoreCase = true) }
		?.let { it.get(null) as? Color }
}

fun textAttrFromDoc(docComment: PsiComment) =
	docComment.text.removePrefix("///").trim().let(::rgbToAttributes)

@Language("RegExp")
@JvmField
val lexicalRegex = Regex("\\A(\\d+)\\^(\\d+)\\^(.*)$")

@Language("RegExp")
@JvmField
val errLineRegex = Regex("\\A\\s+-->\\s+(\\d+):(\\d+)$")

@Language("RegExp")
@JvmField
val errInfoRegex = Regex("\\A\\s+=\\s+(\\p{all}*)$")

class LivePreviewAnnotator : Annotator {
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		if (element !is LivePreviewFile) return
		highlight(element, { range, info ->
			holder.newAnnotation(HighlightSeverity.ERROR, info.orEmpty())
					.range(range)
					.create()
		}) { range, rule, attributes ->
			holder.newAnnotation(HighlightSeverity.INFORMATION, rule)
					.enforcedTextAttributes(attributes)
					.range(range)
					.create()
		}
	}
}

inline fun highlight(
	element: LivePreviewFile,
	err: (TextRange, String?) -> Unit,
	okk: (TextRange, String, TextAttributes) -> Unit
) {
	val project = element.project
	val dom = PsiDocumentManager.getInstance(project).getDocument(element)
		?: return
	val pestFile = element.pestFile ?: return
	val ruleName = element.ruleName ?: return
	val rules = pestFile.rules().map { it.name to it }.toMap()
	val regexes = pestFile.childrenWithLeaves
		.filter { it.elementType == PestTokenType.LINE_REGEX_COMMENT }
		.mapNotNull {
			val expr = it.text.removePrefix("//!")
			val (color, regex) = expr.split(":", limit = 2)
				.takeIf { it.size == 2 } ?: return@mapNotNull null
			val c = rgbToAttributes(color) ?: return@mapNotNull null
			val r = try {
				Regex(regex.trim())
			} catch (_: Exception) {
				return@mapNotNull null
			}
			c to r
		}
		.toList()
	if (rules.isEmpty()) return
	if (pestFile.errors.any()) return
	if (pestFile.availableRules.none()) return
	val vm = pestFile.vm
	when (val res = try {
		vm.renderCode(ruleName, element.text)
	} catch (e: Exception) {
		vm.reboot()
		vm.loadVM(pestFile.text)
		vm.renderCode(ruleName, element.text)
	}) {
		is Rendering.Err -> run {
			val errorLines = res.msg.lines()
			val firstLine = errorLines.firstOrNull() ?: return@run null
			val lastLine = errorLines.lastOrNull() ?: return@run null
			val length = dom.textLength
			if (length == 0) return@run null
			val (_, lineS, colS) = errLineRegex.matchEntire(firstLine)?.groupValues
				?: return@run null
			val line = lineS.toIntOrNull() ?: return@run null
			val col = colS.toIntOrNull() ?: return@run null
			val lineStart = dom.getLineStartOffset(line - 1)
			val offset = lineStart + col - 1
			val range = if (offset >= dom.textLength) TextRange(length - 1, length)
			else TextRange(offset, offset + 1)
			val errorMsg = errInfoRegex.matchEntire(lastLine)?.run { groupValues[1] }
			err(range, errorMsg)
		} ?: ApplicationManager.getApplication().invokeLater {
			val panel = JPanel().apply { add(JBTextArea().apply { text = res.msg }) }
			val editor = EditorFactory.getInstance()
				.getEditors(dom, project)
				.firstOrNull()
				?: return@invokeLater
			val factory = JBPopupFactory.getInstance()
			factory
				.createBalloonBuilder(panel)
				.setTitle(PestBundle.message("pest.annotator.live-preview.error.title"))
				.setFillColor(JBColor.RED)
				.createBalloon()
				.show(factory.guessBestPopupLocation(editor), Balloon.Position.below)
		}
		is Rendering.Ok -> res.lexical.mapNotNull(lexicalRegex::matchEntire).forEach {
			val (_, start, end, rule) = it.groupValues
			val psiRule = rules[rule] ?: return@forEach
			val attributes = psiRule.docComment?.let(::textAttrFromDoc)
				?: regexes.asSequence().firstOrNull { (_, regex) ->
					regex.matchEntire(psiRule.name) != null
				}?.first ?: return@forEach
			val range = TextRange(start.toInt(), end.toInt())
			okk(range, rule, attributes)
		}
	}
}
