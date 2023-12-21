package network.radicle.jetbrains.radiclejetbrainsplugin.patches.review;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.diff.AddCommentGutterIconRenderer;
import com.intellij.collaboration.ui.codereview.diff.DiffEditorGutterIconRendererFactory;
import com.intellij.collaboration.ui.codereview.diff.EditorComponentInlaysManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsActions;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.HashMap;
import java.util.Map;

public class PatchDiffEditorGutterIconFactory implements DiffEditorGutterIconRendererFactory {
    private final EditorComponentInlaysManager editorComponentInlaysManager;
    private final ObservableThreadModel observableThreadModel;
    private final RadPatch patch;

    public PatchDiffEditorGutterIconFactory(EditorComponentInlaysManager editorComponentInlaysManager,
                                            RadPatch patch, ObservableThreadModel observableThreadModel) {
        this.editorComponentInlaysManager = editorComponentInlaysManager;
        this.patch = patch;
        this.observableThreadModel = observableThreadModel;
    }

    @NotNull
    @Override
    public AddCommentGutterIconRenderer createCommentRenderer(int editorLine) {
        return new CommentIconRenderer(editorLine);
    }

    public class CommentIconRenderer extends AddCommentGutterIconRenderer {
        private final int editorLine;
        private final Map<Integer, Map<JComponent, Disposable>> inlay = new HashMap<>();

        private CommentIconRenderer(int line) {
            this.editorLine = line;
        }

        @Override
        public @Nullable AnAction getClickAction() {
            var map = inlay.get(editorLine);
            if (map == null) {
                return null;
            }
            var component = map.keySet().toArray()[0];
            return new FocusInlayAction((JComponent) component);
        }

        @Override
        public @Nullable ActionGroup getPopupMenuActions() {
            return new DefaultActionGroup(createComment());
        }

        public AddSingleCommentAction createComment() {
            return new AddSingleCommentAction();
        }

        @Override
        public int getLine() {
            return this.editorLine;
        }

        @Override
        public void disposeInlay() {

        }

        public class AddSingleCommentAction extends InlayAction {
            public final PatchDiffEditorComponentsFactory patchDiffEditorReviewComponentsFactory;

            public AddSingleCommentAction() {
                super(RadicleBundle.message("singleComment"));
                this.patchDiffEditorReviewComponentsFactory = new PatchDiffEditorComponentsFactory(patch, editorLine, observableThreadModel);
            }

            @Override
            protected JComponent createComponent(HideCommentComponent hideComponent) {
                return this.patchDiffEditorReviewComponentsFactory.createSingleCommentComponent(hideComponent);
            }
        }

        private static class FocusInlayAction extends DumbAwareAction {
            private final JComponent component;

            private FocusInlayAction(JComponent component) {
                this.component = component;
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                    CollaborationToolsUIUtil.INSTANCE.focusPanel(component);
            }
        }

        private abstract class InlayAction extends DumbAwareAction {

            public InlayAction(@Nullable @NlsActions.ActionText String text) {
                super(text);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                HideCommentComponent hideComponent = (line) -> {
                    var map = inlay.get(editorLine);
                    if (map == null) {
                        return;
                    }
                    var component = (JComponent) map.keySet().toArray()[0];
                    var disposable = map.get(component);
                    if (disposable != null) {
                        Disposer.dispose(disposable);
                        inlay.remove(editorLine);
                    }
                };
                var component = this.createComponent(hideComponent);
                var myDisposable = editorComponentInlaysManager.insertAfter(editorLine, component, 0, null);
                CollaborationToolsUIUtil.INSTANCE.focusPanel(component);
                if (myDisposable != null) {
                    inlay.put(editorLine, Map.of(component, myDisposable));
                }
            }

            protected abstract JComponent createComponent(HideCommentComponent hideComponent);
        }
    }

    public interface HideCommentComponent {
        void hide(int line);
    }
}
