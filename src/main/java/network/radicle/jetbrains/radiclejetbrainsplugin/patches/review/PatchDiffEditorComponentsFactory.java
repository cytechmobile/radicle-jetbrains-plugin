package network.radicle.jetbrains.radiclejetbrainsplugin.patches.review;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.openapi.application.ApplicationManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.ThreadModel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class PatchDiffEditorComponentsFactory {
    private final ObservableThreadModel observableThreadModel;
    private final RadicleProjectApi api;
    private final RadPatch patch;
    private final int editorLine;
    private PatchReviewThreadComponentFactory patchReviewThreadComponentFactory;

    public PatchDiffEditorComponentsFactory(RadPatch radPatch, int editorLine, ObservableThreadModel observableThreadModel) {
        this.api = radPatch.project.getService(RadicleProjectApi.class);
        this.editorLine = editorLine;
        this.patch = radPatch;
        this.observableThreadModel = observableThreadModel;
    }

    public JComponent createThreadComponent(ThreadModel threads) {
        patchReviewThreadComponentFactory = new PatchReviewThreadComponentFactory(patch, observableThreadModel);
        var thread = patchReviewThreadComponentFactory.createThread(threads);
        return CodeReviewCommentUIUtil.INSTANCE.createEditorInlayPanel(thread);
    }

    public JComponent createSingleCommentComponent(PatchDiffEditorGutterIconFactory.HideCommentComponent hideComponent) {
        var panelHandle = new EditablePanelHandler.PanelBuilder(patch.project, new JPanel(),
                RadicleBundle.message("patch.comment", "Comment"),
                new SingleValueModel<>(""), field -> {
            var location = new RadDiscussion.Location(observableThreadModel.getFileName(), "ranges", (editorLine), (editorLine));
            var res = this.api.addPatchComment(patch, field.getText(), location, field.getEmbedList());
            boolean success = res != null;
            if (success) {
                observableThreadModel.update(patch);
                ApplicationManager.getApplication().invokeLater(() -> {
                    hideComponent.hide(editorLine);
                });
            }
            return success;
        }).enableDragAndDrop(false).hideCancelAction(true).build();
        panelHandle.showAndFocusEditor();
        var builder = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.COMPACT, integer ->
                new SingleValueModel<>(RadicleIcons.DEFAULT_AVATAR), panelHandle.panel);
        var component = builder.build();
        return CodeReviewCommentUIUtil.INSTANCE.createEditorInlayPanel(component);
    }

    public PatchReviewThreadComponentFactory getPatchReviewThreadComponentFactory() {
        return patchReviewThreadComponentFactory;
    }
}
