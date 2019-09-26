package rs.pest.livePreview

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.vfs.VfsUtil
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.awt.Color
import java.io.File

fun defaultPreviewToHtml(file: LivePreviewFile, indicator: ProgressIndicator) {
	val fileName = "${file.pestFile?.name}-${file.ruleName}.html"
	val ioFile = File(fileName)
	ioFile.writer().use {
		toHtml(file, it, indicator)
		it.flush()
	}
	VfsUtil.findFileByIoFile(ioFile, true)
}

private fun toHtml(file: LivePreviewFile, html: Appendable, indicator: ProgressIndicator) {
	indicator.text = "Initializing highlighting info"
	indicator.fraction = 0.0
	html.appendHTML().html {
		head { charset("UTF-8") }
		comment("Generated with love by IntelliJ-Pest")
		val chars = file.textToCharArray()
		val highlights = arrayOfNulls<Pair<Color?, String>>(chars.size)
		highlight(file, { range, err ->
			for (i in range.startOffset..range.endOffset) highlights[i] = null to err.orEmpty()
		}) { range, info, attributes ->
			for (i in range.startOffset..range.endOffset)
				highlights[i] = attributes.foregroundColor to info
		}
		indicator.text = "Writing html"
		indicator.fraction = 0.2
		body { pre { ReadAction.run<ProcessCanceledException> { render(highlights, chars, indicator) } } }
	}
}

private fun PRE.render(highlights: Array<Pair<Color?, String>?>, chars: CharArray, indicator: ProgressIndicator) {
	ProgressIndicatorProvider.checkCanceled()
	for ((i, highlight) in highlights.withIndex()) {
		indicator.fraction = 0.2 + (i.toDouble() / chars.size) * 0.8
		if (highlight == null) +chars[i].toString()
		else {
			val color = highlight.first
			if (color == null) {
				TODO("highlight parse failures")
			} else font(color) { +chars[i].toString() }
		}
	}
}

private class FONT(consumer: TagConsumer<*>, color: Color) : HTMLTag("font",
	consumer, mapOf("color" to "#${color.rgb}"), inlineTag = true, emptyTag = false
), HtmlInlineTag

private fun PRE.font(color: Color, block: FONT.() -> Unit = {}) {
	FONT(consumer, color).visit(block)
}
