package rs.pest.livePreview

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.popup.PopupFactoryImpl
import icons.PestIcons
import rs.pest.PestFile
import rs.pest.interop.Lib
import rs.pest.vm.PestUtil
import javax.swing.SwingConstants

fun livePreview(file: PestFile) {
	val project = file.project
	val virtualFile = LightVirtualFile("${file.name}.preview", LivePreviewFileType, "")
	val psiFile = PsiManagerEx.getInstanceEx(project).findFile(virtualFile) as? LivePreviewFile ?: return
	psiFile.pestFile = file
	val editorManager = FileEditorManagerEx.getInstanceEx(project)
	PsiDocumentManager.getInstance(project).getDocument(file)?.addDocumentListener(object : DocumentListener {
		var previousBalloon: Balloon? = null
		override fun documentChanged(event: DocumentEvent) {
			if (PsiTreeUtil.hasErrorElements(file)) return
			val component = editorManager
				.getEditors(virtualFile)
				.firstOrNull()
				?.component
				?: return
			psiFile.vm.reboot()
			val (works, messages) = psiFile.vm.loadVM(file.text)
			previousBalloon?.hide()
			if (works) {
				//language=HTML
				PopupFactoryImpl.getInstance()
					.createHtmlTextBalloonBuilder("<html><h2>Pest VM load successfully.</h2></html>", MessageType.INFO, null)
			} else {
				//language=HTML
				PopupFactoryImpl.getInstance()
					.createHtmlTextBalloonBuilder(messages.joinToString(
						separator = "</li><li>",
						prefix = "<html><h2>Pest VM load failed.</h2><ul><li>",
						postfix = "</li></ul></html>"), MessageType.ERROR, null)
			}.createBalloon().also { previousBalloon = it }.showInCenterOf(component)
		}
	})
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
	val vm = Lib(PestUtil(1926417 + 1919810))
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
