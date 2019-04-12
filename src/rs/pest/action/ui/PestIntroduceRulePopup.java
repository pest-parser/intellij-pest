package rs.pest.action.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rs.pest.PestBundle;
import rs.pest.psi.PestExpression;

import javax.swing.*;

@SuppressWarnings("NullableProblems")
public abstract class PestIntroduceRulePopup extends InplaceVariableIntroducer<PestExpression> {
	protected @NotNull JPanel mainPanel;
	protected @NotNull JRadioButton atomic;
	protected @NotNull JRadioButton compoundAtomic;
	protected @NotNull JRadioButton silent;
	protected @NotNull JRadioButton nonAtomic;
	protected @NotNull JRadioButton normal;

	public PestIntroduceRulePopup(PsiNamedElement elementToRename, Editor editor, Project project, @Nullable PestExpression expr) {
		super(elementToRename, editor, project, PestBundle.message("pest.actions.extract.rule.popup.title"), new PestExpression[0], expr);
	}
}
