package network.radicle.jetbrains.radiclejetbrainsplugin.patches.review;

import com.google.common.base.Strings;
import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.util.LineRange;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.EDT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.ThreadModel;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.stream.Collectors;

public class ObservableThreadModel {
    private static final Logger logger = LoggerFactory.getLogger(ObservableThreadModel.class);
    private final EventDispatcher<ChangeListener> changeEventDispatcher = EventDispatcher.create(ChangeListener.class);
    private final RadicleProjectApi api;
    private final Change change;

    public ObservableThreadModel(Change change, Project project) {
        this.api = project.getService(RadicleProjectApi.class);
        this.change = change;
    }

    public void addChangesListener(ChangeListener listener) {
        changeEventDispatcher.addListener(listener);
    }

    public void update(RadPatch patch) {
        if (EDT.isCurrentThreadEdt()) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> updateEditorCommentsUi(patch));
        } else {
            updateEditorCommentsUi(patch);
        }
    }

    private void update(List<ThreadModel> threadModelList) {
        changeEventDispatcher.getMulticaster().clearThreads();
        for (var thread : threadModelList) {
            changeEventDispatcher.getMulticaster().threadAdded(thread);
        }
    }

    public List<LineRange> getModifiedLines() {
        try {
            if (change.getBeforeRevision() == null && change.getAfterRevision() == null) {
                return List.of();
            }
            var beforeContent = change.getBeforeRevision() != null ? change.getBeforeRevision().getContent() : "";
            var afterContent = change.getAfterRevision() != null ? change.getAfterRevision().getContent() : "";
            var fragments = ComparisonManager.getInstance().compareLines(Strings.nullToEmpty(beforeContent), Strings.nullToEmpty(afterContent),
                    ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE);
            return fragments.stream().map(fragment -> new LineRange(fragment.getStartLine1(), fragment.getEndLine2()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Unable to find modified lines");
            return List.of();
        }
    }

    public List<ThreadModel> getInlineComments(RadPatch patch) {
        var lastRevision = patch.revisions.get(patch.revisions.size() - 1);
        var discussions = lastRevision.discussions().stream().filter(discussion -> discussion.isReviewComment() &&
                discussion.location.path.equals(getFileName())).toList();
        var groupedDiscussions = new ArrayList<ThreadModel>();
        for (var discussion : discussions) {
            var line = discussion.location.start;
            if (isCommentInModifiedLine(line)) {
                var threadModel = new ThreadModel(line, discussion);
                groupedDiscussions.add(threadModel);
            }
        }
        return groupedDiscussions;
    }

    public String getFileName() {
        return change.getVirtualFile().getName();
    }

    public interface ChangeListener extends EventListener {
        void threadAdded(ThreadModel threadModel);
        void clearThreads();
    }

    private void updateEditorCommentsUi(RadPatch patch) {
        var fetched = this.api.fetchPatch(patch.projectId, patch.repo, patch.id);
        boolean success = fetched != null;
        if (success) {
            var inlineComments = getInlineComments(fetched);
            ApplicationManager.getApplication().invokeLater(() -> {
                update(inlineComments);
            });
        }
    }

    private boolean isCommentInModifiedLine(int line) {
        for (var modifiedLine : getModifiedLines()) {
            if (line >= modifiedLine.start && line <= modifiedLine.end) {
                return true;
            }
        }
        return false;
    }
}
