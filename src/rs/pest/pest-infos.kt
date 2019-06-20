package rs.pest

import com.intellij.CommonBundle
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import icons.PestIcons
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import rs.pest.livePreview.Lib
import rs.pest.livePreview.LivePreviewFile
import rs.pest.psi.impl.PestGrammarRuleMixin
import java.util.*

object PestFileType : LanguageFileType(PestLanguage.INSTANCE) {
	override fun getDefaultExtension() = PEST_EXTENSION
	override fun getName() = PestBundle.message("pest.name")
	override fun getIcon() = PestIcons.PEST_FILE
	override fun getDescription() = PestBundle.message("pest.name.description")
}

class PestFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, PestLanguage.INSTANCE) {
	override fun getFileType() = PestFileType
	override fun subtreeChanged() {
		super.subtreeChanged()
		rulesCache = null
	}

	/** This information is from the Psi system. */
	private var rulesCache: List<PestGrammarRuleMixin>? = null
	var livePreviewFile: MutableList<LivePreviewFile> = mutableListOf()
	/** This information is from Pest VM. */
	var errors: Sequence<Pair<TextRange, String>> = emptySequence()
	/** This information is from Pest VM. */
	var availableRules: Sequence<String> = emptySequence()
	val vm = Lib(1926417 + 1919810)
	fun rebootVM() = vm.reboot()
	fun reloadVM(code: String = text) = vm.loadVM(code)
	var isDocumentListenerAdded = false
	fun rules() = rulesCache ?: calcRules().also { rulesCache = it }
	fun livePreviewFile() = livePreviewFile.also { it.retainAll(PsiElement::isValid) }
	private fun calcRules() = children.filterIsInstance<PestGrammarRuleMixin>()
}

class PestFileTypeFactory : FileTypeFactory() {
	override fun createFileTypes(consumer: FileTypeConsumer) {
		consumer.consume(PestFileType, PEST_EXTENSION)
	}
}

object PestBundle {
	@NonNls
	private const val BUNDLE = "rs.pest.pest-bundle"
	private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

	@JvmStatic
	fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
		CommonBundle.message(bundle, key, *params)
}
