package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.components.panels.ListLayout;
import com.intellij.util.ui.JBUI;
import net.miginfocom.layout.CC;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class Utils {
    private static final String SPLIT_CHAR = ":";
    public static JComponent getVerticalPanel(int gap) {
        return new JPanel(ListLayout.vertical(gap, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
    }

    public static void addListPanel(JPanel panel, LabeledListPanelHandle<?> handle) {
        panel.add(handle.getTitleLabel(), new CC().alignY("top").width("30"));
        panel.add(handle.getPanel(), new CC().minWidth("0").growX().pushX().wrap());
    }

    public static JComponent getHorizontalPanel(int gap) {
        return new JPanel(ListLayout.horizontal(gap, ListLayout.Alignment.START, ListLayout.GrowPolicy.GROW));
    }

    public static JPanel descriptionPanel(MarkDownEditorPaneFactory editorPane, Project project) {
        var panelHandle = new EditablePanelHandler.PanelBuilder(project, editorPane.htmlEditorPane(),
                RadicleBundle.message("issue.change.title"),
                new SingleValueModel<>(editorPane.getRawContent()), (editedTitle) -> true).build();
        var contentPanel = panelHandle.panel;
        var b = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                i -> new SingleValueModel<>(new ImageIcon()), contentPanel);
        b.setMaxContentWidth(Integer.MAX_VALUE);
        return (JPanel) b.build();
    }

    public static String formatPatchId(String patchId) {
        return patchId.substring(0, 6);
    }

    public static String formatDid(String did) {
        var didStr = "did:key:";
        var parts = did.split(SPLIT_CHAR);
        if (parts.length != 3) {
            return did;
        }
        var id = parts[2];
        if (id.length() < 6) {
            return id;
        }
        var firstPart = id.substring(0, 6);
        var secondPart = id.substring(id.length() - 6);
        return didStr + firstPart + "..." + secondPart;
    }

    public static LinkLabel<?> createLinkLabel(String text, LinkListener listener) {
        var linkLabel = new LinkLabel<>(text, null);
        linkLabel.setListener(listener, null);
        linkLabel.setBorder(JBUI.Borders.emptyRight(8));
        return linkLabel;
    }

    public static class RemoveNewLineFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (!string.contains("\n")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (!text.contains("\n")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}
