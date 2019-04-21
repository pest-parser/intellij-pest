package rs.pest.action.ui;

import com.intellij.ui.components.labels.LinkLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

@SuppressWarnings("NullableProblems")
public abstract class PestIdeBridgeInfo {
	protected @NotNull JPanel mainPanel;
	protected @NotNull LinkLabel<Object> websiteLink;
	protected @NotNull LinkLabel<Object> crateLink;
	protected @NotNull JLabel versionLabel;
	protected @NotNull JLabel authorLabel;
	protected @NotNull JLabel descriptionLabel;
}
