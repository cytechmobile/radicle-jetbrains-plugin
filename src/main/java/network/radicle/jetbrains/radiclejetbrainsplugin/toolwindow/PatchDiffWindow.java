package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.diff.DiffContext;
import com.intellij.diff.DiffExtension;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer;
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer;
import com.intellij.diff.util.Side;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchComponentFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.ObservableThreadModel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.PatchDiffEditorGutterIconFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.controllers.PatchEditorCommentableRangesController;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.controllers.PatchReviewThreadsController;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;

public class PatchDiffWindow extends DiffExtension {
    private PatchDiffEditorGutterIconFactory patchDiffEditorGutterIconFactory;
    private PatchReviewThreadsController patchReviewThreadsController;
    private EditorImpl myEditor;

    @Override
    public void onViewerCreated(FrameDiffTool.@NotNull DiffViewer viewer, @NotNull DiffContext context, @NotNull DiffRequest request) {
        RadPatch patch = context.getUserData(PatchComponentFactory.PATCH_DIFF);
        if (patch == null) {
            return;
        }
        Change change = request.getUserData(ChangeDiffRequestProducer.CHANGE_KEY);
        if (change == null) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
           var radicleProjectService = patch.project.getService(RadicleProjectService.class);
           var radDetails = new RadSelf(patch.project).getRadSelfDetails();
            radicleProjectService.setRadDetails(radDetails);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (viewer instanceof TwosideTextDiffViewer twosideTextDiffViewer) {
                    setupEditor(twosideTextDiffViewer.getEditor(Side.LEFT), change, patch, true);
                    setupEditor(twosideTextDiffViewer.getEditor(Side.RIGHT), change, patch, false);
                } else if (viewer instanceof SimpleOnesideDiffViewer simpleOnesideDiffViewer) {
                    var isLeft = simpleOnesideDiffViewer.getSide().isLeft();
                    setupEditor(simpleOnesideDiffViewer.getEditor(), change, patch, isLeft);
                }
            });
        });
    }

    private void setupEditor(Editor editor, Change change, RadPatch patch, boolean isLeft) {
        myEditor = (EditorImpl) editor;
        var observableThreadModel = new ObservableThreadModel(change, patch.project, isLeft);
        patchDiffEditorGutterIconFactory = new PatchDiffEditorGutterIconFactory(myEditor, patch, observableThreadModel);
        new PatchEditorCommentableRangesController(patchDiffEditorGutterIconFactory, myEditor, observableThreadModel.getModifiedLines());
        patchReviewThreadsController = new PatchReviewThreadsController(myEditor, observableThreadModel, patch);
        observableThreadModel.update(patch);
    }

    public EditorImpl getEditor() {
        return myEditor;
    }

    public PatchDiffEditorGutterIconFactory getPatchDiffEditorGutterIconFactory() {
        return patchDiffEditorGutterIconFactory;
    }

    public PatchReviewThreadsController getPatchReviewThreadsController() {
        return patchReviewThreadsController;
    }

}
