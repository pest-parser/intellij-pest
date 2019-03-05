package rs.pest.rust

import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import org.rust.lang.core.psi.ext.RsLitExprMixin

class InlineGrammarInjector : LanguageInjector {
	override fun getLanguagesToInject(host: PsiLanguageInjectionHost, places: InjectedLanguagePlaces) {
		if (host is RsLitExprMixin) {

		}
	}
}
