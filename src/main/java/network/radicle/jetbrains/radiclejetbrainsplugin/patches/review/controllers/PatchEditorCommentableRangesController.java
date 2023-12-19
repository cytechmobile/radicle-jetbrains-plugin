package network.radicle.jetbrains.radiclejetbrainsplugin.patches.review.controllers;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.diff.DiffEditorGutterIconRendererFactory;
import com.intellij.collaboration.ui.codereview.diff.EditorRangesController;
import com.intellij.diff.util.LineRange;
import com.intellij.openapi.editor.ex.EditorEx;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PatchEditorCommentableRangesController extends EditorRangesController {
    private final List<LineRange> lineRanges;

    public PatchEditorCommentableRangesController(@NotNull DiffEditorGutterIconRendererFactory gutterIconRendererFactory,
                                                  @NotNull EditorEx editor, List<LineRange> lineRanges) {
        super(gutterIconRendererFactory, editor);
        this.lineRanges = lineRanges;
        init();
    }

    public void init() {
        var commentableRanges = new SingleValueModel<>(lineRanges);
        commentableRanges.addAndInvokeListener(ranges -> {
            for (var line : ranges) {
                PatchEditorCommentableRangesController.super.markCommentableLines(line);
            }
            return Unit.INSTANCE;
        });
    }
}
