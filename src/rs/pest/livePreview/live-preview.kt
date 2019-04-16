package rs.pest.livePreview

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextParserDefinition
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType
import icons.PestIcons

object LivePreviewFileType : LanguageFileType(LivePreviewLanguage.INSTANCE) {
	override fun getDefaultExtension() = "PEST_EXTENSION"
	override fun getName() = LivePreviewLanguage.INSTANCE.displayName
	override fun getIcon() = PestIcons.PEST_FILE
	override fun getDescription() = LivePreviewLanguage.INSTANCE.displayName
}

class LivePreviewFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, LivePreviewLanguage.INSTANCE) {
	override fun getFileType() = LivePreviewFileType
}

class LivePreviewParserDefinition : PlainTextParserDefinition() {
	override fun getFileNodeType() = FILE
	override fun createFile(viewProvider: FileViewProvider) = LivePreviewFile(viewProvider)
	companion object {
		@JvmField val FILE = IFileElementType(LivePreviewLanguage.INSTANCE)
	}
}
