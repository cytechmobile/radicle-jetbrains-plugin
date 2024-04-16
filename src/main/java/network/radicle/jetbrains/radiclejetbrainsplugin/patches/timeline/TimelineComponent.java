package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.NamedColorUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchProposalPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.DragAndDropField;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.TimelineComponentFactory.createTimeLineItem;

public class TimelineComponent {
    private final TimelineComponentFactory componentsFactory;
    private final RadPatch radPatch;
    private final SingleValueModel<RadPatch> radPatchModel;

    private JPanel headerPanel;
    private JComponent commentPanel;
    private JComponent revisionSection;
    private final RadicleProjectApi api;

    public TimelineComponent(PatchProposalPanel patchProposalPanel, PatchVirtualFile file) {
        this.radPatchModel = file.getPatchModel();
        this.radPatch = file.getPatchModel().getValue();
        componentsFactory = new TimelineComponentFactory(patchProposalPanel, radPatchModel, file);
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
        revisionSection = componentsFactory.createTimeline();
        timelinePanel.add(revisionSection);

        var horizontalPanel = Utils.getHorizontalPanel(8);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radDetails = getCurrentRadDetails();
            if (radDetails != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    var selfAuthor = new RadAuthor(radDetails.nodeId, radDetails.alias);
                    var self = selfAuthor.generateLabelText();
                    var commentSection = createTimeLineItem(getCommentField().panel, horizontalPanel, self, null);
                    commentPanel = commentSection;
                    timelinePanel.add(commentSection);
                }, ModalityState.any());
            }
        });

        var mainPanel = new Wrapper();
        var scrollPanel = ScrollPaneFactory.createScrollPane(timelinePanel, true);
        scrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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

    public boolean createComment(DragAndDropField field) {
        if (Strings.isNullOrEmpty(field.getText())) {
            return false;
        }
        var ok = api.addPatchComment(radPatch, field.getText(), null, field.getEmbedList());
        if (ok != null) {
            radPatchModel.setValue(ok);
            return true;
        }
        return false;
    }

    private EditablePanelHandler getCommentField() {
        var panelHandle = new EditablePanelHandler.PanelBuilder(radPatch.repo.getProject(), new JPanel(),
                RadicleBundle.message("patch.comment"), new SingleValueModel<>(""),
                this::createComment)
                .hideCancelAction(true)
                .closeEditorAfterSubmit(false)
                .build();
        panelHandle.showAndFocusEditor();
        return panelHandle;
    }

    private JComponent getHeader() {
        // final var title = CodeReviewTitleUIUtil.INSTANCE.createTitleText(radPatch.title, radPatch.id, "", "");
        final var patchWebLink = HtmlChunk.link(api.getPatchWebUrl(radPatch), radPatch.id).attr("title", radPatch.title)
                .wrapWith(HtmlChunk.font(ColorUtil.toHex(NamedColorUtil.getInactiveTextColor())));
        final var title = new HtmlBuilder().appendRaw(radPatch.title).nbsp().append(patchWebLink).toString();

        var headerTitle = new BaseHtmlEditorPane();
        headerTitle.setFont(JBFont.h2().asBold());
        headerTitle.setBody(title);

        var panelHandle = new EditablePanelHandler.PanelBuilder(radPatch.repo.getProject(), headerTitle,
                RadicleBundle.message("patch.proposal.change.title"), new SingleValueModel<>(radPatch.title), (field) -> {
            var edit = new RadPatch(radPatch);
            edit.title = field.getText();
            var edited = api.changePatchTitle(edit);
            final boolean success = edited != null;
            if (success) {
                radPatchModel.setValue(edited);
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
}

