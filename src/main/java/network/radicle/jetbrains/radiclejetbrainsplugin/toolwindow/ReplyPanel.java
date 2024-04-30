package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.google.protobuf.Any;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.ToggleableContainer;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.labels.LinkLabel;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.PatchReviewThreadComponentFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

public abstract class ReplyPanel<T> {
    private final Project project;
    private final RadDiscussion radDiscussion;
    private final SingleValueModel<T> model;
    private SingleValueModel<Boolean> toggleModel = new SingleValueModel<>(false);

    public ReplyPanel(Project project, RadDiscussion radDiscussion, SingleValueModel<T> model) {
        this.project = project;
        this.radDiscussion = radDiscussion;
        this.model = model;
    }

    public JComponent getThreadActionsComponent() {
        return ToggleableContainer.INSTANCE.create(toggleModel, () -> {
            PatchReviewThreadComponentFactory.ReplyAction rep = () -> toggleModel.setValue(true);
            return createCollapsedThreadActionComponent(rep);
        }, () -> createUncollapsedThreadActionsComponent());
    }

    private JComponent createUncollapsedThreadActionsComponent() {
        var panelHandle = new EditablePanelHandler.PanelBuilder(project, new JPanel(),
                RadicleBundle.message("reply", "Reply"),
                new SingleValueModel<>(""), (field) -> {

            var success = this.addReply(field.getText(), field.getEmbedList(), radDiscussion.id);
            if (success) {
                model.setValue(model.getValue());
            }
            return success;
        }).enableDragAndDrop(true)
                .hideCancelAction(false)
                .build();
        panelHandle.addOnCloseListener(() -> toggleModel.setValue(false));
        panelHandle.showAndFocusEditor();
        var builder = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.COMPACT, integer ->
                new SingleValueModel<>(RadicleIcons.DEFAULT_AVATAR), panelHandle.panel);
        return builder.build();
    }

    private JComponent createCollapsedThreadActionComponent(PatchReviewThreadComponentFactory.ReplyAction replyAct) {
        var reply = new LinkLabel<Any>(RadicleBundle.message("reply", "Reply"), null) {
            @Override
            public void doClick() {
                replyAct.reply();
            }
        };
        var horizontalPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        horizontalPanel.add(reply);
        return horizontalPanel;
    }

    public abstract boolean addReply(String comment, List<Embed> embedList, String replyToId);
}
