package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageComponentFactory;
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import git4idea.GitCommit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchProposalPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.EmojiPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.MarkDownEditorPaneFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getHorizontalPanel;
import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getVerticalPanel;

public class TimelineComponentFactory {
    private static final String PATTERN_FORMAT = "dd/MM/yyyy HH:mm";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());
    private final RadPatch patch;
    private final CountDownLatch latch = new CountDownLatch(1);
    private JComponent descSection;
    private Map<String, List<GitCommit>> groupedCommits;
    private final PatchProposalPanel patchProposalPanel;
    private final SingleValueModel<RadPatch> patchModel;
    private final RadicleProjectApi api;
    private RadDetails radDetails;
    private JPanel emojiJPanel;
    private EmojiPanel<RadPatch> emojiPanel;
    private final PatchVirtualFile file;
    private JComponent commentPanel;
    private JComponent mainPanel;

    public TimelineComponentFactory(PatchProposalPanel patchProposalPanel, SingleValueModel<RadPatch> patchModel, PatchVirtualFile file) {
        this.file = file;
        this.patch = patchModel.getValue();
        this.patchProposalPanel = patchProposalPanel;
        this.patchModel = patchModel;
        this.api = patch.project.getService(RadicleProjectApi.class);
    }

    public JComponent createDescSection() {
        var description = !Strings.isNullOrEmpty(patch.getLatestRevision().description()) ? patch.getLatestRevision().description() :
                RadicleBundle.message("noDescription");
        var editorPane = new MarkDownEditorPaneFactory(description, patch.project, patch.projectId, file);
        descSection = Utils.descriptionPanel(editorPane, patch.project);
        return descSection;
    }

    public JComponent createRevisionSection() {
        mainPanel = getVerticalPanel(0);
        var loadingIcon = new JLabel(new AnimatedIcon.Default());
        mainPanel.add(loadingIcon);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            groupedCommits = patch.calculateCommits();
            radDetails = api.getCurrentIdentity();
            latch.countDown();
            ApplicationManager.getApplication().invokeLater(() -> {
                loadingIcon.setVisible(false);
                if (groupedCommits == null) {
                    RadAction.showErrorNotification(patch.repo.getProject(),
                            RadicleBundle.message("radCliError"),
                            RadicleBundle.message("errorCalculatingPatchProposalCommits"));
                    return;
                }
                if (radDetails == null) {
                    RadAction.showErrorNotification(patch.repo.getProject(),
                            RadicleBundle.message("radCliError"),
                            RadicleBundle.message("identityDetailsError"));
                    return;
                }
                for (var rev : patch.revisions) {
                    var contentPanel = getVerticalPanel(4);
                    var patchCommits = groupedCommits.get(rev.id());
                    Collections.reverse(patchCommits);
                    contentPanel.setOpaque(false);
                    var horizontalPanel = getHorizontalPanel(8);
                    horizontalPanel.setOpaque(false);
                    var revAuthor = !Strings.isNullOrEmpty(rev.author().alias) ? rev.author().alias : rev.author().id;
                    var item = createTimeLineItem(contentPanel, horizontalPanel, RadicleBundle.message("revisionPublish", rev.id(), revAuthor),
                            rev.timestamp());
                    mainPanel.add(item);
                    mainPanel.add(createCommentSection(List.of(rev)));
                }
            });
        });
        return mainPanel;
    }

    private String findMessage(String replyTo) {
        for (var rev : patch.revisions) {
            for (var com : rev.discussions()) {
                if (com.id.equals(replyTo)) {
                    return com.body;
                }
            }
        }
        return "";
    }

    private JComponent createCommentSection(List<RadPatch.Revision> revisions) {
        var verticalPanel = getVerticalPanel(0);
        for (var rev : revisions) {
            for (var com : rev.discussions()) {
                var textHtmlEditor = new BaseHtmlEditorPane();
                textHtmlEditor.setOpaque(false);
                var message = com.body;
                if (!Strings.isNullOrEmpty(com.replyTo)) {
                    var replyToMessage = findMessage(com.replyTo);
                    message = "<div><div style=\"border-left: 2px solid black;\">" +
                            " <div style=\"margin-left:10px\">" + replyToMessage + "</div>\n" +
                            "</div><div style=\"margin-top:5px\">" + message + "</div></div>";
                }
                var panel = new BorderLayoutPanel();
                panel.setOpaque(false);
                var editorPane = new MarkDownEditorPaneFactory(message, patch.project, patch.projectId, file);
                panel.addToCenter(StatusMessageComponentFactory.INSTANCE.create(editorPane.htmlEditorPane(), StatusMessageType.WARNING));
                emojiPanel = new PatchEmojiPanel(patchModel, com.reactions, com.id, radDetails);
                emojiJPanel = emojiPanel.getEmojiPanel();
                panel.addToBottom(emojiJPanel);
                var panelHandle = new EditablePanelHandler.PanelBuilder(patch.project, panel,
                        RadicleBundle.message("save"), new SingleValueModel<>(message), (field) -> {
                    var edited = api.changePatchComment(rev.id(), com.id, field.getText(), patch, field.getEmbedList());
                    final boolean success = edited != null;
                    if (success) {
                        patchModel.setValue(patch);
                    }
                    return success;
                }).build();
                var contentPanel = panelHandle.panel;
                var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
                actionsPanel.add(CodeReviewCommentUIUtil.INSTANCE.createEditButton(e -> {
                    panelHandle.showAndFocusEditor();
                    return null;
                }));
                commentPanel = createTimeLineItem(contentPanel, actionsPanel, com.author.generateLabelText(), com.timestamp);
                verticalPanel.add(commentPanel);
            }
        }
        return verticalPanel;
    }

    public JPanel getEmojiJPanel() {
        return emojiJPanel;
    }

    public EmojiPanel<RadPatch> getEmojiPanel() {
        return emojiPanel;
    }

    public static JComponent createTimeLineItem(JComponent contentPanel,
                                                JComponent actionsPanel, String title, Instant date) {
        var authorDid = HtmlChunk.link("#",
                title).wrapWith(HtmlChunk.font(ColorUtil.toHtmlColor(UIUtil.getLabelForeground()))).bold();
        var titleText = new HtmlBuilder().append(authorDid)
                .append(HtmlChunk.nbsp())
                .append(date != null ? DATE_TIME_FORMATTER.format(date) : "");
        var titleTextPane = new BaseHtmlEditorPane();
        titleTextPane.setOpaque(false);
        titleTextPane.setBody(titleText.toString());
        titleTextPane.removeHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
        titleTextPane.setForeground(UIUtil.getContextHelpForeground());
        return new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                integer -> new SingleValueModel<>(RadicleIcons.DEFAULT_AVATAR), contentPanel)
                .withHeader(titleTextPane, actionsPanel).build();
    }

    public JComponent getCommentPanel() {
        return commentPanel;
    }

    public JComponent getDescSection() {
        return descSection;
    }

    private class PatchEmojiPanel extends EmojiPanel<RadPatch> {

        public PatchEmojiPanel(SingleValueModel<RadPatch> model, List<Reaction> reactions, String discussionId, RadDetails radDetails) {
            super(model, reactions, discussionId, radDetails);
        }

        @Override
        public RadPatch addEmoji(Emoji emoji, String commentId) {
            var revisionId = findRevisionId(commentId);
            return api.patchCommentReact(patch, commentId, revisionId, emoji.unicode(), true);
        }

        @Override
        public RadPatch removeEmoji(String emojiUnicode, String commentId) {
            var revisionId = findRevisionId(commentId);
            return api.patchCommentReact(patch, commentId, revisionId, emojiUnicode, false);
        }

        @Override
        public void notifyEmojiChanges(String emojiUnicode, String commentId, boolean isAdded) {
            var revisionId = findRevisionId(commentId);
            var revision = patch.findRevision(revisionId);
            if (revision != null) {
                var discussion = revision.findDiscussion(commentId);
                if (discussion != null) {
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
                    updatePanel(patch);
                }
            }
        }

        public void updatePanel(RadPatch updatePatch) {
            var panel = (JPanel) mainPanel.getComponent(2);
            panel.removeAll();
            for (var rev : updatePatch.revisions) {
                panel.add(createCommentSection(List.of(rev)));
            }
            panel.revalidate();
            panel.repaint();
        }

        private String findRevisionId(String commentId) {
            String revisionId = "";
            for (var rev : patch.revisions) {
                for (var com : rev.discussions()) {
                    if (com.id.equals(commentId)) {
                        revisionId = rev.id();
                        break;
                    }
                }
            }
            return revisionId;
        }
    }
}

