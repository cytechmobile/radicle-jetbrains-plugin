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
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.EmojiPanel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
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

    public IssueComponent(SingleValueModel<RadIssue> issueModel) {
        this.radIssue = issueModel.getValue();
        this.issueModel = issueModel;
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
            var com = discussionList.get(i);
            var textHtmlEditor = new BaseHtmlEditorPane();
            textHtmlEditor.setOpaque(false);
            var message = com.body;
            if (!Strings.isNullOrEmpty(com.replyTo)) {
                var replyToMessage = findMessage(com.replyTo, discussionList);
                message = "<div><div style=\"border-left: 2px solid black;\">" +
                        " <div style=\"margin-left:10px\">" + replyToMessage + "</div>\n" +
                        "</div><div style=\"margin-top:5px\">" + message + "</div></div>";
            }
            textHtmlEditor.setBody("<html><body>" + message + "</body></html>");
            var horizontalPanel = getHorizontalPanel(8);
            horizontalPanel.setOpaque(false);
            var contentPanel = new BorderLayoutPanel();
            contentPanel.setOpaque(false);
            contentPanel.add(StatusMessageComponentFactory.INSTANCE.create(textHtmlEditor, StatusMessageType.WARNING));
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
        public RadIssue selectedEmoji(Emoji emoji, String discussionId) {
            return api.issueCommentReact(radIssue, discussionId, emoji.getUnicode());
        }

        @Override
        public RadIssue unselectEmoji(String emojiUnicode, String discussionId) {
            //TODO
            return null;
        }
    }

    private EditablePanelHandler getCommentField() {
        var panelHandle = new EditablePanelHandler.PanelBuilder(radIssue.repo.getProject(), new JPanel(),
                RadicleBundle.message("issue.comment", "Comment"), new SingleValueModel<>(""), this::createComment)
                .hideCancelAction(true)
                .closeEditorAfterSubmit(false)
                .build();
        panelHandle.showAndFocusEditor();
        return panelHandle;
    }

    public boolean createComment(String comment) {
        if (Strings.isNullOrEmpty(comment)) {
            return true;
        }
        var edited = api.addIssueComment(radIssue, comment);
        final boolean success = edited != null;
        if (success) {
            issueModel.setValue(edited);
        }
        return true;
    }

    private JComponent getDescription() {
        var bodyIssue = radIssue.discussion.size() > 0 ? radIssue.discussion.get(0).body : "";
        bodyIssue = bodyIssue.replaceAll("\\\\n", "<br>");
        var descriptionEditor = new BaseHtmlEditorPane();
        descriptionEditor.setFont(JBFont.h4().asPlain());
        descriptionEditor.setBody("<html>" + bodyIssue + "</html>");
        var panelHandle = new EditablePanelHandler.PanelBuilder(radIssue.repo.getProject(), descriptionEditor,
                RadicleBundle.message("issue.change.title", "change title"),
                new SingleValueModel<>(bodyIssue), (editedTitle) -> true).build();
        var contentPanel = panelHandle.panel;
        var b = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                i -> new SingleValueModel<>(new ImageIcon()), contentPanel);
        b.withHeader(contentPanel, null);
        descPanel = (JPanel) b.build();
        return descPanel;
    }

    private JComponent getHeader() {
        final var title = CodeReviewTitleUIUtil.INSTANCE.createTitleText(radIssue.title, radIssue.id, "", "");
        var headerTitle = new BaseHtmlEditorPane();
        headerTitle.setFont(JBFont.h2().asBold());
        headerTitle.setBody(title);
        var panelHandle = new EditablePanelHandler.PanelBuilder(radIssue.repo.getProject(), headerTitle,
                RadicleBundle.message("issue.change.title", "change title"),
                new SingleValueModel<>(radIssue.title), (editedTitle) -> {
            var issue = new RadIssue(radIssue);
            issue.title = editedTitle;
            var edited = api.changeIssueTitle(issue);
            final boolean success = edited != null;
            if (success) {
                issueModel.setValue(edited);
            }
            return success;
        }).build();
        var contentPanel = panelHandle.panel;
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        actionsPanel.add(CodeReviewCommentUIUtil.INSTANCE.createEditButton(e -> {
            panelHandle.showAndFocusEditor();
            return null;
        }));

        var b = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                i -> new SingleValueModel<>(RadicleIcons.RADICLE), contentPanel);
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
