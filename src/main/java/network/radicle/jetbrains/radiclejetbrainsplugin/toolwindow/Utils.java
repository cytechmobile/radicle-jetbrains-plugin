package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.ide.ClipboardSynchronizer;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.components.panels.ListLayout;
import com.intellij.util.Function;
import com.intellij.util.ui.JBUI;
import net.miginfocom.layout.CC;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.List;

public class Utils {
    private static final String SPLIT_CHAR = ":";
    public static JComponent getVerticalPanel(int gap) {
        return new JPanel(ListLayout.vertical(gap, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
    }

    public static RadDiscussion updateRadDiscussionModel(RadPatch patch, String emojiUnicode, String commentId, RadDetails radDetails, boolean isAdded) {
        var revisionId = patch.findRevisionId(commentId);
        var revision = patch.findRevision(revisionId);
        if (revision != null) {
            var discussion = revision.findDiscussion(commentId);
            if (discussion != null) {
                return updateRadDiscussionModel(discussion, emojiUnicode, radDetails, isAdded);
            }
        }
        return null;
    }

    public static RadDiscussion updateRadDiscussionModel(RadDiscussion discussion, String emojiUnicode, RadDetails radDetails, boolean isAdded) {
        var reaction = discussion.findReaction(emojiUnicode);
        var author = reaction != null ? reaction.findAuthor(radDetails.did) : null;
        if (isAdded && author == null) {
            if (reaction == null) {
                // If the reaction does not exist, add a new reaction with the author
                discussion.reactions.add(new Reaction(emojiUnicode, List.of(new RadAuthor(radDetails.did, radDetails.alias))));
            } else {
                // If the reaction exists, add the author to the existing reaction
                reaction.authors().add(new RadAuthor(radDetails.did, radDetails.alias));
            }
        } else if (!isAdded && author != null) {
            if (reaction.authors().size() > 1) {
                // If the reaction has multiple authors, remove the current author
                reaction.authors().remove(author);
            } else {
                // If the reaction has only one author, remove the entire reaction from the discussion
                discussion.reactions.remove(reaction);
            }
        }
        return discussion;
    }

    public static void addListPanel(JPanel panel, LabeledListPanelHandle<?> handle) {
        panel.add(handle.getTitleLabel(), new CC().alignY("top").width("30"));
        panel.add(handle.getPanel(), new CC().minWidth("0").growX().pushX().wrap());
    }

    public static JComponent getHorizontalPanel(int gap) {
        return new JPanel(ListLayout.horizontal(gap, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
    }

    public static String formatReplyMessage(String message, String replyMessage) {
        replyMessage = Strings.isNullOrEmpty(replyMessage) ? "&lt; DELETED COMMENT &gt;" : replyMessage;
        return "<div><div style=\"border-left: 2px solid black;\">" +
                " <div style=\"margin-left:10px\">" + replyMessage + "</div>\n" +
                "</div><div style=\"margin-top:5px\">" + message + "</div></div>";
    }

    public static JPanel descriptionPanel(
            String content, Project project, String radProjectId, VirtualFile file,
            String changeTitle, Function<DragAndDropField, Boolean> editAction) {
        final var panel = Utils.getVerticalPanel(1);
        final var editorPane = new MarkDownEditorPaneFactory(content, project, radProjectId, file, panel);
        panel.add(editorPane.htmlEditorPane());
        var panelHandle = new EditablePanelHandler.PanelBuilder(project, panel, RadicleBundle.message(changeTitle),
                new SingleValueModel<>(editorPane.getRawContent()), editAction).oneLine(false).enableDragAndDrop(false).build();
        var contentPanel = panelHandle.panel;
        var b = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                i -> new SingleValueModel<>(new ImageIcon()), contentPanel);
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        actionsPanel.add(CodeReviewCommentUIUtil.INSTANCE.createEditButton(e -> {
            panelHandle.showAndFocusEditor();
            return null;
        }));
        var malakia = new JPanel(null);
        malakia.setOpaque(false);
        b.withHeader(malakia, actionsPanel);
        //b.setMaxContentWidth(Integer.MAX_VALUE);
        return (JPanel) b.build();
    }

    public static String formatId(String id) {
        if (id.length() < 6) {
            return id;
        }
        return id.substring(0, 6);
    }

    public static String formatDid(String did) {
        String id = did;
        String didStr = "";
        if (id.startsWith("did:key:")) {
            didStr = "did:key:";
            var parts = did.split(SPLIT_CHAR);
            if (parts.length != 3) {
                id = did;
            } else {
                id = parts[2];
            }
        }
        if (id.length() < 12) {
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

    public static class CopyButton extends JButton {
        public CopyButton(String contents) {
            setAction(getCopyAction(contents));
        }

        private Action getCopyAction(String contents) {
            ActionManager actionManager = ActionManager.getInstance();
            if (actionManager == null) {
                return null;
            }
            AnAction action = actionManager.getAction(IdeActions.ACTION_COPY);
            if (action == null) {
                return null;
            }
            return new AbstractAction("", action.getTemplatePresentation().getIcon()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    StringSelection content = new StringSelection(contents);
                    ClipboardSynchronizer.getInstance().setContent(content, content);
                }
            };
        }
    }
}
