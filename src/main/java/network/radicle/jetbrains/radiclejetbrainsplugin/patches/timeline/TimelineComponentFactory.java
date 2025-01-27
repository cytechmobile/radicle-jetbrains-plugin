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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchProposalPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.EmojiPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.MarkDownEditorPaneFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ReplyPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TimelineComponentFactory {
    private static final String PATTERN_FORMAT = "dd/MM/yyyy HH:mm";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());
    private static final String JPANEL_PREFIX_NAME = "COMMENT_";
    private final RadPatch patch;
    private final CountDownLatch latch = new CountDownLatch(1);
    private JComponent descSection;
    private final PatchProposalPanel patchProposalPanel;
    private final SingleValueModel<RadPatch> patchModel;
    private RadDetails radDetails;
    private JPanel emojiJPanel;
    private EmojiPanel<RadPatch> emojiPanel;
    private final PatchVirtualFile file;
    private JComponent commentPanel;
    private JComponent mainPanel;
    private JComponent replyPanel;
    private final RadicleCliService cli;

    public TimelineComponentFactory(PatchProposalPanel patchProposalPanel, SingleValueModel<RadPatch> patchModel, PatchVirtualFile file) {
        this.file = file;
        this.patch = patchModel.getValue();
        this.patchProposalPanel = patchProposalPanel;
        this.patchModel = patchModel;
        this.cli = patch.project.getService(RadicleCliService.class);
    }

    public JComponent createDescSection() {
        var description = patch.getLatestNonEmptyRevisionDescription();
        if (Strings.isNullOrEmpty(description)) {
            description = RadicleBundle.message("noDescription");
        }
        descSection = Utils.descriptionPanel(description, patch.project, patch.radProject.id, file, "patch.proposal.change.description", f -> {
            var newDesc = f.getText();
            if (Strings.isNullOrEmpty(newDesc)) {
                return false;
            }
            var edited = cli.changePatchTitleDescription(patch, patch.title, newDesc);
            final boolean success = edited != null;
            if (success) {
                patchModel.setValue(edited);
            }
            return success;
        });
        return descSection;
    }

    public JComponent createTimeline() {
        mainPanel = Utils.getVerticalPanel(0);
        var loadingIcon = new JLabel(new AnimatedIcon.Default());
        mainPanel.add(loadingIcon);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            radDetails = cli.getCurrentIdentity();
            latch.countDown();
            ApplicationManager.getApplication().invokeLater(() -> {
                loadingIcon.setVisible(false);
                if (radDetails == null) {
                    RadAction.showErrorNotification(patch.repo.getProject(),
                            RadicleBundle.message("radCliError"),
                            RadicleBundle.message("identityDetailsError"));
                    return;
                }
                var timelineEvents = patch.getTimelineEvents();
                for (var event : timelineEvents) {
                    JComponent myComponent = null;
                    if (event instanceof RadPatch.Revision) {
                        myComponent = createRevisionComponent((RadPatch.Revision) event);
                    } else if (event instanceof RadPatch.Review) {
                        myComponent = createReviewComponent((RadPatch.Review) event);
                    } else if (event instanceof RadDiscussion) {
                        myComponent = createCommentComponent((RadDiscussion) event);
                    }
                    if (myComponent != null) {
                        mainPanel.add(myComponent);
                    }
                }
            });
        });
        return mainPanel;
    }

    private String findMessage(String replyTo) {
        for (var rev : patch.getRevisionList()) {
            for (var com : rev.getDiscussions()) {
                if (com.id.equals(replyTo)) {
                    return com.body;
                }
            }
        }
        return "";
    }

    private JComponent createRevisionComponent(RadPatch.Revision rev) {
        var contentPanel = Utils.getVerticalPanel(4);
        contentPanel.setOpaque(false);
        var horizontalPanel = Utils.getHorizontalPanel(8);
        horizontalPanel.setOpaque(false);
        var revAuthor = rev.author().generateLabelText(cli);
        return createTimeLineItem(contentPanel, horizontalPanel, RadicleBundle.message("revisionPublish", rev.id(), revAuthor), rev.timestamp());
    }

    private JComponent createReviewComponent(RadPatch.Review review) {
        var reviewPanel = Utils.getVerticalPanel(0);
        var textHtmlEditor = new BaseHtmlEditorPane();
        textHtmlEditor.setOpaque(false);
        var message = Strings.nullToEmpty(review.summary());
        var panel = new BorderLayoutPanel();
        panel.setOpaque(false);
        var myPanel = Utils.getVerticalPanel(1);
        var editorPane = new MarkDownEditorPaneFactory(message, patch.project, patch.radProject.id, file, myPanel);
        myPanel.setOpaque(false);
        myPanel.add(editorPane.htmlEditorPane());
        var verdictMsg = review.verdict() == RadPatch.Review.Verdict.ACCEPT ? RadicleBundle.message("approved") : RadicleBundle.message("requestChanges");
        var color = review.verdict() == RadPatch.Review.Verdict.ACCEPT ? StatusMessageType.SUCCESS : StatusMessageType.WARNING;

        myPanel.add(StatusMessageComponentFactory.INSTANCE.create(new JLabel(verdictMsg), color));
        panel.addToCenter(myPanel);
        var panelHandle = new EditablePanelHandler.PanelBuilder(patch.project, panel,
                RadicleBundle.message("save"), new SingleValueModel<>(message), (field) -> true).enableDragAndDrop(false).build();
        var contentPanel = panelHandle.panel;
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        var item = createTimeLineItem(contentPanel, actionsPanel, review.author().generateLabelText(cli), review.timestamp());
        reviewPanel.add(item);
        return reviewPanel;
    }

    private JComponent createCommentComponent(RadDiscussion com) {
        var myMainPanel = Utils.getVerticalPanel(0);
        var textHtmlEditor = new BaseHtmlEditorPane();
        textHtmlEditor.setOpaque(false);
        var message = com.body;
        if (!Strings.isNullOrEmpty(com.replyTo)) {
            var replyToMessage = findMessage(com.replyTo);
            message = Utils.formatReplyMessage(message, replyToMessage);
        }
        var panel = new BorderLayoutPanel();
        panel.setOpaque(false);
        var editorPane = new MarkDownEditorPaneFactory(message, patch.project, patch.radProject.id, file, panel);
        panel.addToCenter(StatusMessageComponentFactory.INSTANCE.create(editorPane.htmlEditorPane(), StatusMessageType.WARNING));
        emojiPanel = new PatchEmojiPanel(patchModel, com.reactions, com.id, radDetails);
        var verticalPanel = Utils.getVerticalPanel(5);
        verticalPanel.setOpaque(false);
        emojiJPanel = emojiPanel.getEmojiPanel();
        verticalPanel.add(emojiJPanel);
        replyPanel = new MyReplyPanel(patch.project, com, patchModel).getThreadActionsComponent();
        verticalPanel.add(replyPanel);
        panel.addToBottom(verticalPanel);
        if (com.isReviewComment()) {
            var infoPanel = Utils.getHorizontalPanel(0);
            infoPanel.setOpaque(false);
            var msg = RadicleBundle.message("comment.on", com.location.path, com.location.start);
            if (!patch.isDiscussionBelongedToLatestRevision(com)) {
                msg = msg + " (OUTDATED)";
            }
            infoPanel.add(new JLabel(msg));
            panel.addToTop(infoPanel);
        }
        var panelHandle = new EditablePanelHandler.PanelBuilder(patch.project, panel,
                RadicleBundle.message("save"), new SingleValueModel<>(com.body), (field) -> {
            var edited = cli.editPatchComment(patch, patch.findRevisionId(com.id), com.id, field.getText(), field.getEmbedList());
            final boolean success = edited != null;
            if (success) {
                patchModel.setValue(patch);
            }
            return success;
        }).enableDragAndDrop(true).build();
        var contentPanel = panelHandle.panel;
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        var self = cli.getCurrentIdentity();
        if (self != null && com.author != null && com.author.contains(self.nodeId)) {
            final var editButton = CodeReviewCommentUIUtil.INSTANCE.createEditButton(e -> {
                panelHandle.showAndFocusEditor();
                return null;
            });
            final var deleteButton = CodeReviewCommentUIUtil.INSTANCE.createDeleteCommentIconButton(e -> {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    var revisionId = patch.findRevisionId(com.id);
                    var success = cli.deletePatchComment(patch, revisionId, com.id);
                    if (success != null) {
                        patchModel.setValue(patch);
                    }
                });
                return Unit.INSTANCE;
            });
            actionsPanel.add(editButton);
            actionsPanel.add(deleteButton);
        }
        commentPanel = createTimeLineItem(contentPanel, actionsPanel, com.author == null ? "" : com.author.generateLabelText(cli), com.timestamp);
        myMainPanel.add(commentPanel);
        myMainPanel.setName(JPANEL_PREFIX_NAME + com.id);
        return myMainPanel;
    }

    public JPanel getEmojiJPanel() {
        return emojiJPanel;
    }

    public EmojiPanel<RadPatch> getEmojiPanel() {
        return emojiPanel;
    }

    public static JComponent createTimeLineItem(JComponent contentPanel, JComponent actionsPanel, String title, Instant date) {
        var authorDid = HtmlChunk.link("#", title).wrapWith(HtmlChunk.font(ColorUtil.toHtmlColor(UIUtil.getLabelForeground()))).bold();
        var titleText = new HtmlBuilder().append(authorDid).append(HtmlChunk.nbsp()).append(date != null ? DATE_TIME_FORMATTER.format(date) : "");
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

    public JComponent getReplyPanel() {
        return replyPanel;
    }

    private class MyReplyPanel extends ReplyPanel<RadPatch> {

        public MyReplyPanel(Project project, RadDiscussion radDiscussion, SingleValueModel<RadPatch> model) {
            super(project, radDiscussion, model);
        }

        @Override
        public boolean addReply(String comment, List<Embed> list, String replyToId) {
            return cli.createPatchComment(patch, patch.getLatestRevision().id(), comment, replyToId, null, list);
        }
    }

    private class PatchEmojiPanel extends EmojiPanel<RadPatch> {
        public PatchEmojiPanel(SingleValueModel<RadPatch> model, List<Reaction> reactions, String discussionId, RadDetails radDetails) {
            super(patch.project, model, reactions, discussionId, radDetails);
        }

        @Override
        public RadPatch addEmoji(Emoji emoji, String commentId) {
            return cli.patchCommentReact(patch, commentId, emoji.unicode(), true);
        }

        @Override
        public RadPatch removeEmoji(String emojiUnicode, String commentId) {
            return cli.patchCommentReact(patch, commentId, emojiUnicode, false);
        }

        @Override
        public void notifyEmojiChanges(String emojiUnicode, String commentId, boolean isAdded) {
            var updatedDiscussion = Utils.updateRadDiscussionModel(patch, emojiUnicode, commentId, radDetails, isAdded);
            if (updatedDiscussion != null) {
                updatePanel(updatedDiscussion);
            }
        }

        public void updatePanel(RadDiscussion discussion) {
            var children = mainPanel.getComponents();
            JComponent commentComponent = null;
            for (var child : children) {
                if (child.getName() != null && child.getName().equals(JPANEL_PREFIX_NAME + discussion.id)) {
                    commentComponent = (JComponent) child;
                    break;
                }
            }
            if (commentComponent != null) {
                commentComponent.removeAll();
                commentComponent.add(createCommentComponent(discussion).getComponent(0));
                commentComponent.revalidate();
                commentComponent.repaint();
            }

        }
    }
}

