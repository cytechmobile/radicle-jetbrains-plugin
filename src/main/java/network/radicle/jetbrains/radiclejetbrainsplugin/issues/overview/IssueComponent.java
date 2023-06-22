package network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTitleUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBFont;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.swing.*;

import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.TimelineComponentFactory.createTimeLineItem;
import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.createCommentSection;

public class IssueComponent {
    private final RadIssue radIssue;
    private final SingleValueModel<RadIssue> issueModel;
    private BaseHtmlEditorPane headerTitle;
    private final RadicleProjectApi api;

    public IssueComponent(SingleValueModel<RadIssue> issueModel) {
        this.radIssue = issueModel.getValue();
        this.issueModel = issueModel;
        api = radIssue.project.getService(RadicleProjectApi.class);
    }

    public JComponent create() {
        var mainPanel = new Wrapper();
        var issueContainer = Utils.getVerticalPanel(2);

        issueContainer.add(getHeader());
        issueContainer.add(getDescription());
        issueContainer.add(createCommentSection(radIssue.discussion));

        var horizontalPanel = Utils.getHorizontalPanel(8);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radDetails = api.getCurrentIdentity();
            if (radDetails != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    var commentSection = createTimeLineItem(getCommentField().panel, horizontalPanel, radDetails.did, null);
                    issueContainer.add(commentSection);
                }, ModalityState.any());
            }
        });

        mainPanel.add(issueContainer);
        return mainPanel;
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
        return b.build();
    }

    private JComponent getHeader() {
        final var title = CodeReviewTitleUIUtil.INSTANCE.createTitleText(radIssue.title, radIssue.id, "", "");
        headerTitle = new BaseHtmlEditorPane();
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
        return b.build();
    }
}
