package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.codereview.diff.EditorComponentInlaysManager;
import com.intellij.diff.DiffContext;
import com.intellij.diff.DiffExtension;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer;
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer;
import com.intellij.diff.util.Side;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchComponentFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.PatchDiffEditorGutterIconFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.ObservableThreadModel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.controllers.PatchEditorCommentableRangesController;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.controllers.PatchReviewThreadsController;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;

public class PatchDiffWindow extends DiffExtension {
    private PatchDiffEditorGutterIconFactory patchDiffEditorGutterIconFactory;
    private PatchReviewThreadsController patchReviewThreadsController;
    private EditorImpl editor;

    @Override
    public void onViewerCreated(FrameDiffTool.@NotNull DiffViewer viewer, @NotNull DiffContext context, @NotNull DiffRequest request) {
        RadPatch patch = context.getUserData(PatchComponentFactory.PATCH_DIFF);
        editor = findEditor(viewer);
        if (editor != null && patch != null) {
            Change change = request.getUserData(ChangeDiffRequestProducer.CHANGE_KEY);
            if (change == null) {
                return;
            }
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
               var radicleProjectService = patch.project.getService(RadicleProjectService.class);
               var radDetails = new RadSelf(patch.project).getRadSelfDetails();
               radicleProjectService.setRadDetails(radDetails);
               ApplicationManager.getApplication().invokeLater(() -> {
                   var observableThreadModel = new ObservableThreadModel(change, patch.project);
                   var editorComponentInlaysManager = new EditorComponentInlaysManager(editor);
                   patchDiffEditorGutterIconFactory = new PatchDiffEditorGutterIconFactory(editorComponentInlaysManager, patch, observableThreadModel);
                   new PatchEditorCommentableRangesController(patchDiffEditorGutterIconFactory, editor, observableThreadModel.getModifiedLines());
                   patchReviewThreadsController = new PatchReviewThreadsController(editorComponentInlaysManager, observableThreadModel, patch);
                   observableThreadModel.update(patch);
               });
            });
        }
    }

    private EditorImpl findEditor(FrameDiffTool.DiffViewer viewer) {
        if (viewer instanceof TwosideTextDiffViewer myEditor) {
            return (EditorImpl) myEditor.getEditor(Side.RIGHT);
        }
        if (viewer instanceof SimpleOnesideDiffViewer myEditor) {
            return (EditorImpl) myEditor.getEditor();
        }
        return null;
    }

    public EditorImpl getEditor() {
        return editor;
    }

    public PatchDiffEditorGutterIconFactory getPatchDiffEditorGutterIconFactory() {
        return patchDiffEditorGutterIconFactory;
    }

    public PatchReviewThreadsController getPatchReviewThreadsController() {
        return patchReviewThreadsController;
    }

}
