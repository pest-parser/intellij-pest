package rs.pest

import com.intellij.CommonBundle
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import icons.PestIcons
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
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

	private var rulesCache: MutableMap<String, PestGrammarRuleMixin>? = null
	fun rules() = rulesCache ?: calcRules().also { rulesCache = it }
	fun rule(name: String) = rules()[name]
	private fun calcRules() = children.filterIsInstance<PestGrammarRuleMixin>().run {
		val map = hashMapOf<String, PestGrammarRuleMixin>()
		forEach { rule -> rule.name?.let {  map[it] = rule } }
		map
	}
}

class PestFileTypeFactory : FileTypeFactory() {
	override fun createFileTypes(consumer: FileTypeConsumer) {
		consumer.consume(PestFileType, PEST_EXTENSION)
	}
}

class PestContext : TemplateContextType(PEST_CONTEXT_ID, PEST_LANGUAGE_NAME) {
	override fun isInContext(file: PsiFile, offset: Int) = file.fileType == PestFileType
}

object PestBundle {
	@NonNls private const val BUNDLE = "rs.pest.pest-bundle"
	private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

	@JvmStatic
	fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
		CommonBundle.message(bundle, key, *params)
}
