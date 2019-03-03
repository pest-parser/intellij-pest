package rs.pest;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

import static rs.pest.Pest_constantsKt.PEST_LANGUAGE_NAME;

/**
 * @author ice1000
 */
public class PestLanguage extends Language {
	public static final @NotNull
	PestLanguage INSTANCE = new PestLanguage();

	private PestLanguage() {
		super(PEST_LANGUAGE_NAME, "text/" + PEST_LANGUAGE_NAME);
	}
}
