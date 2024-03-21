package network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.controllers;

import com.intellij.collaboration.ui.codereview.diff.EditorComponentInlaysManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.util.Disposer;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.ThreadModel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.ObservableThreadModel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.PatchDiffEditorComponentsFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PatchReviewThreadsController {
    private final EditorImpl editor;
    private final ObservableThreadModel observableThreadModel;
    private final HashMap<Integer, List<Disposable>> inlay = new HashMap<>();
    private final RadPatch patch;
    private PatchDiffEditorComponentsFactory patchDiffEditorComponentsFactory;

    public PatchReviewThreadsController(EditorImpl editor, ObservableThreadModel observableThreadModel, RadPatch patch) {
        this.editor = editor;
        this.observableThreadModel = observableThreadModel;
        this.patch = patch;
        init();
    }

    private void init() {
        observableThreadModel.addChangesListener(new ObservableThreadModel.ChangeListener() {
            @Override
            public void threadAdded(ThreadModel threadModel) {
                insertThread(threadModel);
            }

            @Override
            public void clearThreads() {
                for (var lines : inlay.keySet()) {
                    var disposables = inlay.get(lines);
                    for (var disposable : disposables) {
                        Disposer.dispose(disposable);
                    }
                }
            }
        });
    }

    private void insertThread(ThreadModel threadModel) {
        patchDiffEditorComponentsFactory = new PatchDiffEditorComponentsFactory(this.patch, threadModel.getLine(), observableThreadModel);
        final var threadComponent = patchDiffEditorComponentsFactory.createThreadComponent(threadModel);
        final var editorComponentInlaysManager = new EditorComponentInlaysManager(editor);
        final var disposable = editorComponentInlaysManager.insertAfter(threadModel.getLine(), threadComponent, 0, null);
        // var disposable = EditorComponentInlaysUtilKt.insertComponentAfter(editor, threadModel.getLine(), threadComponent, 0, d -> null);
        inlay.computeIfAbsent(threadModel.getLine(), l -> new ArrayList<>()).add(disposable);
    }

    public PatchDiffEditorComponentsFactory getPatchDiffEditorComponentsFactory() {
        return patchDiffEditorComponentsFactory;
    }
}
