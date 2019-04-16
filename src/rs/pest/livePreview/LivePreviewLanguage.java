package rs.pest.livePreview;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

import static rs.pest.Pest_constantsKt.LP_LANGUAGE_NAME;

/**
 * @author ice1000
 */
public class LivePreviewLanguage extends Language {
	public static @NotNull
	LivePreviewLanguage INSTANCE = new LivePreviewLanguage();

	private LivePreviewLanguage() {
		super(LP_LANGUAGE_NAME, "text/" + LP_LANGUAGE_NAME);
	}
}
