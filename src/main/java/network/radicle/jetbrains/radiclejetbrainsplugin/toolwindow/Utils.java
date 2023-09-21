package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.HtmlEditorPaneUtil;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.ide.ui.AntialiasingType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.panels.ListLayout;
import com.intellij.util.ui.ExtendableHTMLViewFactory;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.StyleSheetUtil;
import net.miginfocom.layout.CC;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;

import javax.swing.JEditorPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ImageIcon;

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

    public static JEditorPane htmlEditorPane(String body) {
        var textPane = new JTextPane();
        var editorKit = new HTMLEditorKitBuilder();
        editorKit.withViewFactoryExtensions(ExtendableHTMLViewFactory.Extensions.WORD_WRAP, HtmlEditorPaneUtil.INSTANCE.getCONTENT_TOOLTIP(),
                HtmlEditorPaneUtil.INSTANCE.getINLINE_ICON_EXTENSION(),
                HtmlEditorPaneUtil.INSTANCE.getIMAGES_EXTENSION());
        editorKit.withStyleSheet(StyleSheetUtil.getDefaultStyleSheet());
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setFont(JBFont.h4().asPlain());
        textPane.setEditorKit(editorKit.build());
        textPane.setMargin(JBInsets.emptyInsets());
        GraphicsUtil.setAntialiasingType(textPane, AntialiasingType.getAAHintForSwingComponent());
        textPane.setText(wrapHtml(body));
        return textPane;
    }

    public static JPanel descriptionPanel(String description, Project project, VirtualFile file) {
        var markDown = MarkdownUtil.INSTANCE.generateMarkdownHtml(file, description, project);
        var panelHandle = new EditablePanelHandler.PanelBuilder(project, Utils.htmlEditorPane(markDown),
                RadicleBundle.message("issue.change.title", "change title"),
                new SingleValueModel<>(description), (editedTitle) -> true).build();
        var contentPanel = panelHandle.panel;
        var b = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                i -> new SingleValueModel<>(new ImageIcon()), contentPanel);
        b.setMaxContentWidth(Integer.MAX_VALUE);
        return (JPanel) b.build();
    }

    public static String wrapHtml(String body) {
        return "<html><head></head><body" + body + "</body></html>";
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
}
