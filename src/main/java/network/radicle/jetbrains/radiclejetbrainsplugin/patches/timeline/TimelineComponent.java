package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTitleUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchProposalPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.concurrent.CountDownLatch;

import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.TimelineComponentFactory.createTimeLineItem;

public class TimelineComponent {
    private final TimelineComponentFactory componentsFactory;
    private final RadPatch radPatch;
    private final SingleValueModel<RadPatch> radPatchModel;

    private JPanel headerPanel;
    private JComponent commentPanel;
    private final CountDownLatch latch = new CountDownLatch(1);
    private JComponent revisionSection;
    private final RadicleProjectApi api;

    public TimelineComponent(SingleValueModel<RadPatch> radPatchModel, PatchProposalPanel patchProposalPanel) {
        this.radPatchModel = radPatchModel;
        this.radPatch = radPatchModel.getValue();
        componentsFactory = new TimelineComponentFactory(radPatch, patchProposalPanel);
        api = radPatch.project.getService(RadicleProjectApi.class);
    }

    public JComponent create() {
        var header = getHeader();
        var descriptionWrapper = new Wrapper();
        descriptionWrapper.setOpaque(false);
        descriptionWrapper.setContent(componentsFactory.createDescSection());

        var timelinePanel = Utils.getVerticalPanel(0);
        timelinePanel.setBorder(JBUI.Borders.empty(CodeReviewTimelineUIUtil.VERT_PADDING, 0));

        timelinePanel.setOpaque(false);
        timelinePanel.add(header);
        timelinePanel.add(descriptionWrapper);
        revisionSection = componentsFactory.createRevisionSection();
        timelinePanel.add(revisionSection);

        var horizontalPanel = Utils.getHorizontalPanel(8);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radDetails = getCurrentRadDetails();
            if (radDetails != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    var commentSection = createTimeLineItem(getCommentField().panel, horizontalPanel, radDetails.did, null);
                    commentPanel = commentSection;
                    timelinePanel.add(commentSection);
                }, ModalityState.any());
            }
        });

        var mainPanel = new Wrapper();
        var scrollPanel = ScrollPaneFactory.createScrollPane(timelinePanel, true);
        scrollPanel.setOpaque(false);

        mainPanel.setContent(scrollPanel);
        return mainPanel;
    }

    private RadDetails getCurrentRadDetails() {
        var radSelf = new RadSelf(radPatch.project);
        radSelf.askForIdentity(false);
        var output = radSelf.perform();
        if (RadAction.isSuccess(output)) {
            return new RadDetails(output.getStdoutLines(true));
        }
        return null;
    }

    public boolean createComment(String comment) {
        if (Strings.isNullOrEmpty(comment)) {
            return false;
        }
        var ok = api.addPatchComment(radPatch, comment);
        if (ok != null) {
            radPatchModel.setValue(ok);
            latch.countDown();
            return true;
        }
        return false;
    }

    private EditablePanelHandler getCommentField() {
       var panelHandle = new EditablePanelHandler.PanelBuilder(radPatch.repo.getProject(), new JPanel(),
                RadicleBundle.message("patch.comment", "Comment"), new SingleValueModel<>(""), this::createComment)
                .hideCancelAction(true)
                .closeEditorAfterSubmit(false)
                .build();
        panelHandle.showAndFocusEditor();
        return panelHandle;
    }

    private JComponent getHeader() {
        final var title = CodeReviewTitleUIUtil.INSTANCE.createTitleText(radPatch.title, radPatch.id, "", "");

        var headerTitle = new BaseHtmlEditorPane();
        headerTitle.setFont(JBFont.h2().asBold());
        headerTitle.setBody(title);

        var panelHandle = new EditablePanelHandler.PanelBuilder(radPatch.repo.getProject(), headerTitle,
                RadicleBundle.message("patch.proposal.change.title", "change title"), new SingleValueModel<>(radPatch.title), (editedTitle) -> {
            var edit = new RadPatch(radPatch);
            edit.title = editedTitle;
            var edited = api.changePatchTitle(edit);
            final boolean success = edited != null;
            if (success) {
                radPatchModel.setValue(edited);
            }
            latch.countDown();
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

    public JComponent getRevisionSection() {
        return revisionSection;
    }

    public JComponent getCommentPanel() {
        return commentPanel;
    }

    public JPanel getHeaderPanel() {
        return headerPanel;
    }

    public TimelineComponentFactory getComponentsFactory() {
        return componentsFactory;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
