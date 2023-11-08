package network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview;


import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTitleUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageComponentFactory;
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.components.BorderLayoutPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview.editor.IssueVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.EmojiPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.MarkDownEditorPane;
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
    private final RadicleProjectApi api;
    private RadDetails radDetails;
    private JPanel emojiJPanel;
    private EmojiPanel<RadIssue> emojiPanel;
    private final IssueVirtualFile  file;

    public IssueComponent(IssueVirtualFile file) {
        this.file = file;
        this.radIssue = file.getIssueModel().getValue();
        this.issueModel = file.getIssueModel();
        this.api = radIssue.project.getService(RadicleProjectApi.class);
    }

    public JComponent create() {
        var mainPanel = new Wrapper();
        var issueContainer = getVerticalPanel(2);
        issueContainer.add(getHeader());
        issueContainer.add(getDescription());

        var horizontalPanel = getHorizontalPanel(8);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            radDetails = api.getCurrentIdentity();
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
                message = "<div><div style=\"border-left: 2px solid black;\">" +
                        " <div style=\"margin-left:10px\">" + replyToMessage + "</div>\n" +
                        "</div><div style=\"margin-top:5px\">" + message + "</div></div>";
            }
            var horizontalPanel = getHorizontalPanel(8);
            horizontalPanel.setOpaque(false);
            var contentPanel = new BorderLayoutPanel();
            contentPanel.setOpaque(false);
            var editorPane = new MarkDownEditorPane(message, radIssue.project, radIssue.projectId, file);
            contentPanel.add(StatusMessageComponentFactory.INSTANCE.create(editorPane.htmlEditorPane(), StatusMessageType.WARNING));
            emojiPanel = new IssueEmojiPanel(issueModel, com.reactions, com.id, radDetails);
            emojiJPanel = emojiPanel.getEmojiPanel();
            contentPanel.addToBottom(emojiJPanel);
            mainPanel.add(createTimeLineItem(contentPanel, horizontalPanel, com.author.generateLabelText(), com.timestamp));
        }
        return mainPanel;
    }

    public class IssueEmojiPanel extends EmojiPanel<RadIssue> {


        protected IssueEmojiPanel(SingleValueModel<RadIssue> model, List<Reaction> reactions,
                                  String discussionId, RadDetails radDetails) {
            super(model, reactions, discussionId, radDetails);
        }

        @Override
        public RadIssue addEmoji(Emoji emoji, String discussionId) {
            return api.issueCommentReact(radIssue, discussionId, emoji.getUnicode(), true);
        }

        @Override
        public RadIssue removeEmoji(String emojiUnicode, String discussionId) {
            return api.issueCommentReact(radIssue, discussionId, emojiUnicode, false);
        }
    }

    private EditablePanelHandler getCommentField() {
        var panelHandle = new EditablePanelHandler.PanelBuilder(radIssue.repo.getProject(), new JPanel(),
                RadicleBundle.message("issue.comment", "Comment"), new SingleValueModel<>(""),
                (field) -> this.createComment(field.getText(), field.getEmbedList()))
                .hideCancelAction(true)
                .closeEditorAfterSubmit(false)
                .build();
        panelHandle.showAndFocusEditor();
        return panelHandle;
    }

    public boolean createComment(String comment, List<Embed> embedList) {
        if (Strings.isNullOrEmpty(comment)) {
            return true;
        }
        var edited = api.addIssueComment(radIssue, comment, embedList);
        final boolean success = edited != null;
        if (success) {
            issueModel.setValue(edited);
        }
        return true;
    }

    private JComponent getDescription() {
        var bodyIssue = !radIssue.discussion.isEmpty() ? radIssue.discussion.get(0).body : "";
        var editorPane = new MarkDownEditorPane(bodyIssue, radIssue.project, radIssue.projectId, file);
        descPanel = Utils.descriptionPanel(editorPane, radIssue.project);
        return descPanel;
    }

    private JComponent getHeader() {
        final var title = CodeReviewTitleUIUtil.INSTANCE.createTitleText(radIssue.title, radIssue.id, "", "");
        var headerTitle = new BaseHtmlEditorPane();
        headerTitle.setFont(JBFont.h2().asBold());
        headerTitle.setBody(title);
        var panelHandle = new EditablePanelHandler.PanelBuilder(radIssue.repo.getProject(), headerTitle,
                RadicleBundle.message("issue.change.title", "change title"),
                new SingleValueModel<>(radIssue.title), (field) -> {
            var issue = new RadIssue(radIssue);
            issue.title = field.getText();
            var edited = api.changeIssueTitle(issue);
            final boolean success = edited != null;
            if (success) {
                issueModel.setValue(edited);
            }
            return success;
        }).enableDragAndDrop(false).build();
        var contentPanel = panelHandle.panel;
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        actionsPanel.add(CodeReviewCommentUIUtil.INSTANCE.createEditButton(e -> {
            panelHandle.showAndFocusEditor();
            return null;
        }));

        var b = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                i -> new SingleValueModel<>(RadicleIcons.DEFAULT_AVATAR), contentPanel);
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
}
