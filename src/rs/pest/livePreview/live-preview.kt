package rs.pest.livePreview

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.FileViewProvider
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.testFramework.LightVirtualFile
import icons.PestIcons
import rs.pest.PestBundle
import rs.pest.PestFile
import rs.pest.action.ui.RuleSelector
import javax.swing.SwingConstants

fun livePreview(file: PestFile, selected: String) {
	val project = file.project
	val virtualFile = LightVirtualFile("${file.name}.$selected.preview", LivePreviewFileType, "")
	val psiFile = PsiManagerEx.getInstanceEx(project).findFile(virtualFile) as? LivePreviewFile ?: return
	psiFile.pestFile = file
	psiFile.ruleName = selected
	file.livePreviewFile.add(psiFile)
	val editorManager = FileEditorManagerEx.getInstanceEx(project)
	editorManager.currentWindow.split(SwingConstants.HORIZONTAL, false, virtualFile, true)
	editorManager.openFile(virtualFile, true)
}

object LivePreviewFileType : LanguageFileType(LivePreviewLanguage.INSTANCE) {
	override fun getDefaultExtension() = "preview"
	override fun getName() = LivePreviewLanguage.INSTANCE.displayName
	override fun getIcon() = PestIcons.PEST_FILE
	override fun getDescription() = LivePreviewLanguage.INSTANCE.displayName
}

class LivePreviewFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, LivePreviewLanguage.INSTANCE) {
	override fun getFileType() = LivePreviewFileType
	var pestFile: PestFile? = null
	var ruleName: String? = null
}

class LivePreviewParser : PsiParser, LightPsiParser {
	override fun parseLight(root: IElementType?, builder: PsiBuilder?) {
		parse(root ?: return, builder ?: return)
	}

	override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
		val mark = builder.mark()
		run {
			@Suppress("NAME_SHADOWING")
			val mark = builder.mark()
			builder.advanceLexer()
			mark.done(LivePreviewElement.TYPE)
		}
		mark.done(root)
		return builder.treeBuilt
	}
}

class LivePreviewElement(node: ASTNode) : ASTWrapperPsiElement(node) {
	companion object Tokens {
		@JvmField
		val TYPE = IElementType("LivePreview Content", LivePreviewLanguage.INSTANCE)
	}
}

class LivePreviewParserDefinition : PlainTextParserDefinition() {
	override fun getFileNodeType() = FILE
	override fun createFile(viewProvider: FileViewProvider) = LivePreviewFile(viewProvider)
	override fun createParser(project: Project?) = LivePreviewParser()
	override fun createElement(node: ASTNode) = LivePreviewElement(node)

	companion object {
		@JvmField
		val FILE = IFileElementType(LivePreviewLanguage.INSTANCE)
	}
}
