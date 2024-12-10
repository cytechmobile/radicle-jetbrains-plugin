package network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageComponentFactory;
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.NamedColorUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview.editor.IssueVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.DragAndDropField;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.EmojiPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.MarkDownEditorPaneFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ReplyPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import java.util.List;

import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.TimelineComponentFactory.createTimeLineItem;
import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getHorizontalPanel;
import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getVerticalPanel;

public class IssueComponent {
    private final RadIssue radIssue;
    private final SingleValueModel<RadIssue> issueModel;
    private JPanel headerPanel;
    private JPanel descPanel;
    private JComponent commentFieldPanel;
    private JComponent commentSection;
    private RadDetails radDetails;
    private JPanel emojiJPanel;
    private EmojiPanel<RadIssue> emojiPanel;
    private final IssueVirtualFile  file;
    private JComponent replyPanel;
    private final RadicleCliService cli;

    public IssueComponent(IssueVirtualFile file) {
        this.file = file;
        this.radIssue = file.getIssueModel().getValue();
        this.issueModel = file.getIssueModel();
        this.cli = radIssue.project.getService(RadicleCliService.class);
    }

    public JComponent create() {
        var mainPanel = new Wrapper();
        var issueContainer = getVerticalPanel(2);
        issueContainer.add(getHeader());
        issueContainer.add(getDescription());

        var horizontalPanel = getHorizontalPanel(8);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            radDetails = cli.getCurrentIdentity();
            if (radDetails != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    commentSection = createCommentSection(radIssue.discussion);
                    issueContainer.add(commentSection);
                    this.commentFieldPanel = createTimeLineItem(getCommentField().panel, horizontalPanel, radDetails.did, null);
                    issueContainer.add(commentFieldPanel);
                }, ModalityState.any());
            }
        });

        var scrollPanel = ScrollPaneFactory.createScrollPane(issueContainer, true);
        scrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.setOpaque(false);
        mainPanel.setContent(scrollPanel);
        return mainPanel;
    }

    public static String findMessage(String replyTo, List<RadDiscussion> discussionList) {
        for (var com : discussionList) {
            if (com.id.equals(replyTo)) {
                return com.body;
            }
        }
        return "";
    }

    public JComponent createCommentSection(List<RadDiscussion> discussionList) {
        var mainPanel = getVerticalPanel(0);
        /* The first discussion is the description of the issue */
        if (discussionList.size() == 1) {
            return mainPanel;
        }
        for (var i = 1; i < discussionList.size(); i++) {
            var issueId = discussionList.get(0).id;
            var com = discussionList.get(i);
            var message = com.body;
            if (!Strings.isNullOrEmpty(com.replyTo) && !com.replyTo.equals(issueId)) {
                var replyToMessage = findMessage(com.replyTo, discussionList);
                message = Utils.formatReplyMessage(message, replyToMessage);
            }
            var panel = new BorderLayoutPanel();
            panel.setOpaque(false);
            var editorPane = new MarkDownEditorPaneFactory(message, radIssue.project, radIssue.projectId, file);
            panel.addToCenter(StatusMessageComponentFactory.INSTANCE.create(editorPane.htmlEditorPane(), StatusMessageType.WARNING));
            emojiPanel = new IssueEmojiPanel(issueModel, com.reactions, com.id, radDetails);
            var verticalPanel = getVerticalPanel(5);
            verticalPanel.setOpaque(false);
            emojiJPanel = emojiPanel.getEmojiPanel();
            verticalPanel.add(emojiJPanel);
            replyPanel = new MyReplyPanel(radIssue.project, com, issueModel).getThreadActionsComponent();
            verticalPanel.add(replyPanel);
            panel.addToBottom(verticalPanel);
            var panelHandle = new EditablePanelHandler.PanelBuilder(radIssue.project, panel,
                    RadicleBundle.message("save"), new SingleValueModel<>(com.body), f -> true).build();
            // TODO: disabling editing issue comment
            /*(field) -> {
                var edited = api.editIssueComment(radIssue, field.getText(), com.id, field.getEmbedList());
                final boolean success = edited != null;
                if (success) {
                    issueModel.setValue(radIssue);
                }
                return success;
            }*/
            var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
            /* actionsPanel.add(CodeReviewCommentUIUtil.INSTANCE.createEditButton(e -> {
                panelHandle.showAndFocusEditor();
                return null;
            })); */
            var contentPanel = panelHandle.panel;
            mainPanel.add(createTimeLineItem(contentPanel, actionsPanel, com.author.generateLabelText(), com.timestamp));
        }
        return mainPanel;
    }

    private EditablePanelHandler getCommentField() {
        var panelHandle = new EditablePanelHandler.PanelBuilder(radIssue.repo.getProject(), new JPanel(),
                RadicleBundle.message("issue.comment"), new SingleValueModel<>(""),
                this::createComment)
                .hideCancelAction(true)
                .enableDragAndDrop(false)
                .closeEditorAfterSubmit(false)
                .build();
        panelHandle.showAndFocusEditor();
        return panelHandle;
    }

    public boolean createComment(DragAndDropField field) {
        if (Strings.isNullOrEmpty(field.getText())) {
            return true;
        }
        var output = cli.createIssueComment(radIssue.repo, radIssue.id, field.getText(), null);
        if (RadAction.isSuccess(output)) {
            issueModel.setValue(radIssue);
        }
        return true;
    }

    private JComponent getDescription() {
        var bodyIssue = radIssue.getDescription();
        var editorPane = new MarkDownEditorPaneFactory(bodyIssue, radIssue.project, radIssue.projectId, file);
        descPanel = Utils.descriptionPanel(editorPane, radIssue.project);
        return descPanel;
    }

    private JComponent getHeader() {
        // final var title = CodeReviewTitleUIUtil.INSTANCE.createTitleText(radIssue.title, radIssue.id, "", "");
        final var issueWebLink = HtmlChunk.link(cli.getIssueWebUrl(radIssue), radIssue.id).attr("title", radIssue.title)
                .wrapWith(HtmlChunk.font(ColorUtil.toHex(NamedColorUtil.getInactiveTextColor())));
        final var title = new HtmlBuilder().appendRaw(radIssue.title).nbsp().append(issueWebLink).toString();
        var headerTitle = new BaseHtmlEditorPane();
        headerTitle.setFont(JBFont.h2().asBold());
        headerTitle.setBody(title);
        var panelHandle = new EditablePanelHandler.PanelBuilder(radIssue.repo.getProject(), headerTitle,
                RadicleBundle.message("issue.change.title"),
                new SingleValueModel<>(radIssue.title), (field) -> {
            // TODO: this will not work
            var edited = cli.changeIssueTitleDescription(radIssue, field.getText(), radIssue.getDescription());
            final boolean success = edited != null;
            if (success) {
                issueModel.setValue(edited);
            }
            return success;
        }).enableDragAndDrop(false).build();
        var contentPanel = panelHandle.panel;
        var b = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                i -> new SingleValueModel<>(RadicleIcons.DEFAULT_AVATAR), contentPanel);
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        actionsPanel.add(CodeReviewCommentUIUtil.INSTANCE.createEditButton(e -> {
            panelHandle.showAndFocusEditor();
            return null;
        }));
        b.withHeader(contentPanel, actionsPanel);

        headerPanel = (JPanel) b.build();
        return headerPanel;
    }

    public EmojiPanel<RadIssue> getEmojiPanel() {
        return emojiPanel;
    }

    public JPanel getEmojiJPanel() {
        return emojiJPanel;
    }

    public JPanel getHeaderPanel() {
        return headerPanel;
    }

    public JPanel getDescPanel() {
        return descPanel;
    }

    public JComponent getCommentFieldPanel() {
        return commentFieldPanel;
    }

    public JComponent getCommentSection() {
        return commentSection;
    }

    public JComponent getReplyPanel() {
        return replyPanel;
    }

    public class MyReplyPanel extends ReplyPanel<RadIssue> {

        public MyReplyPanel(Project project, RadDiscussion radDiscussion,
                            SingleValueModel<RadIssue> model) {
            super(project, radDiscussion, model);
        }

        @Override
        public boolean addReply(String comment, List<Embed> embedList, String replyToId) {
            var output = cli.createIssueComment(radIssue.repo, radIssue.id, comment, replyToId);
            return RadAction.isSuccess(output);
        }
    }

    public class IssueEmojiPanel extends EmojiPanel<RadIssue> {
        protected IssueEmojiPanel(SingleValueModel<RadIssue> model, List<Reaction> reactions, String discussionId, RadDetails radDetails) {
            super(model, reactions, discussionId, radDetails);
        }

        @Override
        public RadIssue addEmoji(Emoji emoji, String discussionId) {
            return cli.issueCommentReact(radIssue, discussionId, emoji.unicode(), true);
        }

        @Override
        public RadIssue removeEmoji(String emojiUnicode, String discussionId) {
            //return api.issueCommentReact(radIssue, discussionId, emojiUnicode, false);
            return cli.issueCommentReact(radIssue, discussionId, emojiUnicode, false);
        }

        @Override
        public void notifyEmojiChanges(String emojiUnicode, String commentId, boolean isAdded) {
            var discussion = radIssue.findDiscussion(commentId);
            if (discussion != null) {
                Utils.updateRadDiscussionModel(discussion, emojiUnicode, radDetails, isAdded);
                updatePanel(radIssue);
            }
        }

        public void updatePanel(RadIssue issue) {
            commentSection.removeAll();
            commentSection.add(createCommentSection(issue.discussion));
            commentSection.revalidate();
            commentSection.repaint();
        }
    }
}
