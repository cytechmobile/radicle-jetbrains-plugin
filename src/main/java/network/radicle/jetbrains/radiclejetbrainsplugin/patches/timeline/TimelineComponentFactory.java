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
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
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

import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getHorizontalPanel;
import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getVerticalPanel;

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
    private final RadicleProjectApi api;
    private RadDetails radDetails;
    private JPanel emojiJPanel;
    private EmojiPanel<RadPatch> emojiPanel;
    private final PatchVirtualFile file;
    private JComponent commentPanel;
    private JComponent mainPanel;
    private JComponent replyPanel;

    public TimelineComponentFactory(PatchProposalPanel patchProposalPanel, SingleValueModel<RadPatch> patchModel, PatchVirtualFile file) {
        this.file = file;
        this.patch = patchModel.getValue();
        this.patchProposalPanel = patchProposalPanel;
        this.patchModel = patchModel;
        this.api = patch.project.getService(RadicleProjectApi.class);
    }

    public JComponent createDescSection() {
        var description = patch.getLatestNonEmptyRevisionDescription();
        if (Strings.isNullOrEmpty(description)) {
            description = RadicleBundle.message("noDescription");
        }
        var editorPane = new MarkDownEditorPaneFactory(description, patch.project, patch.radProject.id, file);
        descSection = Utils.descriptionPanel(editorPane, patch.project);
        return descSection;
    }

    public JComponent createTimeline() {
        mainPanel = getVerticalPanel(0);
        var loadingIcon = new JLabel(new AnimatedIcon.Default());
        mainPanel.add(loadingIcon);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            radDetails = api.getCurrentIdentity();
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
        for (var rev : patch.revisions) {
            for (var com : rev.discussions()) {
                if (com.id.equals(replyTo)) {
                    return com.body;
                }
            }
        }
        return "";
    }

    private JComponent createRevisionComponent(RadPatch.Revision rev) {
        var contentPanel = getVerticalPanel(4);
        contentPanel.setOpaque(false);
        var horizontalPanel = getHorizontalPanel(8);
        horizontalPanel.setOpaque(false);
        var revAuthor = !Strings.isNullOrEmpty(rev.author().alias) ? rev.author().alias : rev.author().id;
        return createTimeLineItem(contentPanel, horizontalPanel, RadicleBundle.message("revisionPublish", rev.id(), revAuthor),
                rev.timestamp());
    }

    private JComponent createReviewComponent(RadPatch.Review review) {
        var reviewPanel = getVerticalPanel(0);
        var textHtmlEditor = new BaseHtmlEditorPane();
        textHtmlEditor.setOpaque(false);
        var message = review.summary();
        var panel = new BorderLayoutPanel();
        panel.setOpaque(false);
        var editorPane = new MarkDownEditorPaneFactory(message, patch.project, patch.radProject.id, file);
        var myPanel = getVerticalPanel(1);
        myPanel.setOpaque(false);
        myPanel.add(editorPane.htmlEditorPane());
        var verdictMsg = review.verdict() == RadPatch.Review.Verdict.ACCEPT ? RadicleBundle.message("approved") : RadicleBundle.message("requestChanges");
        var color = review.verdict() == RadPatch.Review.Verdict.ACCEPT ? StatusMessageType.SUCCESS : StatusMessageType.WARNING;

        myPanel.add(StatusMessageComponentFactory.INSTANCE.create(new JLabel(verdictMsg), color));
        panel.addToCenter(myPanel);
        var panelHandle = new EditablePanelHandler.PanelBuilder(patch.project, panel,
                RadicleBundle.message("save"), new SingleValueModel<>(message), (field) -> true).build();
        var contentPanel = panelHandle.panel;
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        var item = createTimeLineItem(contentPanel, actionsPanel, review.author().generateLabelText(), review.timestamp());
        reviewPanel.add(item);
        return reviewPanel;
    }

    private JComponent createCommentComponent(RadDiscussion com) {
        var myMainPanel = getVerticalPanel(0);
        var textHtmlEditor = new BaseHtmlEditorPane();
        textHtmlEditor.setOpaque(false);
        var message = com.body;
        if (!Strings.isNullOrEmpty(com.replyTo)) {
            var replyToMessage = findMessage(com.replyTo);
            message = Utils.formatReplyMessage(message, replyToMessage);
        }
        var panel = new BorderLayoutPanel();
        panel.setOpaque(false);
        var editorPane = new MarkDownEditorPaneFactory(message, patch.project, patch.radProject.id, file);
        panel.addToCenter(StatusMessageComponentFactory.INSTANCE.create(editorPane.htmlEditorPane(), StatusMessageType.WARNING));
        emojiPanel = new PatchEmojiPanel(patchModel, com.reactions, com.id, radDetails);
        var verticalPanel = getVerticalPanel(5);
        verticalPanel.setOpaque(false);
        emojiJPanel = emojiPanel.getEmojiPanel();
        verticalPanel.add(emojiJPanel);
        replyPanel = new MyReplyPanel(patch.project, com, patchModel).getThreadActionsComponent();
        verticalPanel.add(replyPanel);
        panel.addToBottom(verticalPanel);
        if (com.isReviewComment()) {
            var infoPanel = getHorizontalPanel(0);
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
            var edited = api.changePatchComment(patch.findRevisionId(com.id), com.id, field.getText(), patch, field.getEmbedList());
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

    public JComponent getReplyPanel() {
        return replyPanel;
    }

    private class MyReplyPanel extends ReplyPanel<RadPatch> {

        public MyReplyPanel(Project project, RadDiscussion radDiscussion, SingleValueModel<RadPatch> model) {
            super(project, radDiscussion, model);
        }

        @Override
        public boolean addReply(String comment, List<Embed> list, String replyToId) {
            var res = api.addPatchComment(patch, comment, replyToId, null, list);
            return res != null;
        }
    }

    private class PatchEmojiPanel extends EmojiPanel<RadPatch> {

        public PatchEmojiPanel(SingleValueModel<RadPatch> model, List<Reaction> reactions, String discussionId, RadDetails radDetails) {
            super(model, reactions, discussionId, radDetails);
        }

        @Override
        public RadPatch addEmoji(Emoji emoji, String commentId) {
            var revisionId = patch.findRevisionId(commentId);
            return api.patchCommentReact(patch, commentId, revisionId, emoji.unicode(), true);
        }

        @Override
        public RadPatch removeEmoji(String emojiUnicode, String commentId) {
            var revisionId = patch.findRevisionId(commentId);
            return api.patchCommentReact(patch, commentId, revisionId, emojiUnicode, false);
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

