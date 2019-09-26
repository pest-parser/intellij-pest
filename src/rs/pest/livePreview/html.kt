package rs.pest.livePreview

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.awt.Color

fun toHtml(file: LivePreviewFile, html: Appendable) {
	html.appendHTML().html {
		head { title("${file.pestFile?.name}-${file.ruleName}") }
		comment("Generated with love by IntelliJ-Pest")
		val highlights = arrayOfNulls<Pair<Color?, String>>(file.textLength)
		highlight(file, { range, err ->
			for (i in range.startOffset..range.endOffset) highlights[i] = null to err.orEmpty()
		}) { range, info, attributes ->
			for (i in range.startOffset..range.endOffset)
				highlights[i] = attributes.foregroundColor to info
		}
		body {
			pre {
				TODO()
			}
		}
	}
}
