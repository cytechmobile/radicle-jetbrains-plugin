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
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import git4idea.GitCommit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchProposalPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.EmojiPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.MarkDownEditorPaneFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
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
    private static final String COMMIT_HASH = "commit://";
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

    public TimelineComponentFactory(PatchProposalPanel patchProposalPanel, SingleValueModel<RadPatch> patchModel, PatchVirtualFile file) {
        this.file = file;
        this.patch = patchModel.getValue();
        this.patchProposalPanel = patchProposalPanel;
        this.patchModel = patchModel;
        this.api = patch.project.getService(RadicleProjectApi.class);
    }

    public JComponent createDescSection() {
        var description = !Strings.isNullOrEmpty(patch.description) ? patch.description :
                RadicleBundle.message("noDescription");
        var editorPane = new MarkDownEditorPaneFactory(description, patch.project, patch.projectId, file);
        descSection = Utils.descriptionPanel(editorPane, patch.project);
        return descSection;
    }

    public JComponent createRevisionSection() {
        var mainPanel = getVerticalPanel(0);
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
                    var textHtmlEditor = new BaseHtmlEditorPane();
                    var description = !Strings.isNullOrEmpty(rev.description()) ? rev.description() :
                            RadicleBundle.message("noDescription");
                    textHtmlEditor.setBody(description);
                    contentPanel.add(textHtmlEditor);
                    var patchCommits = groupedCommits.get(rev.id());
                    Collections.reverse(patchCommits);
                    contentPanel.add(StatusMessageComponentFactory.INSTANCE.create(createCommitsSection(patchCommits), StatusMessageType.INFO));
                    contentPanel.setOpaque(false);
                    var horizontalPanel = getHorizontalPanel(8);
                    horizontalPanel.setOpaque(false);
                    var item = createTimeLineItem(contentPanel, horizontalPanel, "Revision " + rev.id() + " was published",
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
        var mainPanel = getVerticalPanel(0);
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
                        RadicleBundle.message("save", "save"), new SingleValueModel<>(message), (field) -> {
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
                mainPanel.add(commentPanel);
            }
        }
        return mainPanel;
    }

    public JPanel getEmojiJPanel() {
        return emojiJPanel;
    }

    public EmojiPanel<RadPatch> getEmojiPanel() {
        return emojiPanel;
    }

    private class PatchEmojiPanel extends EmojiPanel<RadPatch> {

        public PatchEmojiPanel(SingleValueModel<RadPatch> model, List<Reaction> reactions, String discussionId, RadDetails radDetails) {
            super(model, reactions, discussionId, radDetails);
        }

        @Override
        public RadPatch addEmoji(Emoji emoji, String commentId) {
            var revisionId = findRevisionId(commentId);
            return api.patchCommentReact(patch, commentId, revisionId, emoji.getUnicode(), true);
        }

        @Override
        public RadPatch removeEmoji(String emojiUnicode, String commentId) {
            var revisionId = findRevisionId(commentId);
            return api.patchCommentReact(patch, commentId, revisionId, emojiUnicode, false);
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

    public BaseHtmlEditorPane createCommitsSection(List<GitCommit> commits) {
        var builder = new HtmlBuilder();
        for (var commit : commits) {
            builder.append(HtmlChunk.p()
                    .children(HtmlChunk.link(COMMIT_HASH + commit.getId(), commit.getId().asString()),
                            HtmlChunk.nbsp(),
                            HtmlChunk.raw(commit.getFullMessage())));
            builder.append(HtmlChunk.link(commit.getAuthor().getName(), commit.getAuthor().getName()));
            builder.append(HtmlChunk.nbsp());
            builder.append(DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(commit.getAuthorTime())));
        }
        var commitEditor = new BaseHtmlEditorPane();
        commitEditor.setBody(builder.toString());
        commitEditor.removeHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
        commitEditor.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(@NotNull HyperlinkEvent e) {
                var href = e.getDescription();
                patchProposalPanel.selectCommit(href);
            }
        });
        return commitEditor;
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
}

