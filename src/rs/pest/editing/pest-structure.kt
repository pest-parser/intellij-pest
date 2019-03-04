package rs.pest.editing

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import rs.pest.PestFile
import rs.pest.psi.impl.PestGrammarRuleMixin

fun cutText(it: String, textMax: Int) = if (it.length <= textMax) it else "${it.take(textMax)}…"

class PestStructureViewModel(root: PsiFile, editor: Editor?) :
	StructureViewModelBase(root, editor, PestStructureViewElement(root)),
	StructureViewModel.ElementInfoProvider {
	init {
		withSuitableClasses(PestFile::class.java, PestGrammarRuleMixin::class.java)
	}

	override fun shouldEnterElement(o: Any?) = true
	override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
	override fun isAlwaysLeaf(element: StructureViewTreeElement) = element is PestGrammarRuleMixin
}

class PestStructureViewElement(private val root: NavigatablePsiElement) :
	StructureViewTreeElement, ItemPresentation, SortableTreeElement, Navigatable by root {
	override fun getLocationString() = ""
	override fun getIcon(open: Boolean) = root.getIcon(0)
	override fun getPresentableText() = when (root) {
		is PestGrammarRuleMixin -> root.firstChild.text.orEmpty()
		is PestFile -> cutText(root.name, 18)
		else -> "Unknown"
	}

	override fun getPresentation() = this
	override fun getValue() = root
	override fun getAlphaSortKey() = (root as? PsiNamedElement)?.name.orEmpty()
	override fun getChildren() = root
		.children
		.filterIsInstance<PestGrammarRuleMixin>()
		.map { PestStructureViewElement(it) }
		.toTypedArray()
}

class PestStructureViewFactory : PsiStructureViewFactory {
	override fun getStructureViewBuilder(file: PsiFile) = object : TreeBasedStructureViewBuilder() {
		override fun isRootNodeShown() = true
		override fun createStructureViewModel(editor: Editor?) = PestStructureViewModel(file, editor)
	}
}
