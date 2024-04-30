package network.radicle.jetbrains.radiclejetbrainsplugin.patches.review;

import com.google.common.base.Strings;
import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.util.LineRange;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.EDT;
import git4idea.GitUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.ThreadModel;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Optional;

public class ObservableThreadModel {
    private static final Logger logger = LoggerFactory.getLogger(ObservableThreadModel.class);
    private final EventDispatcher<ChangeListener> changeEventDispatcher = EventDispatcher.create(ChangeListener.class);
    private final RadicleProjectApi api;
    private final List<LineRange> modifiedLines;
    private final Change change;
    private final boolean isLeft;

    public ObservableThreadModel(Change change, Project project, boolean isLeft) {
        this.api = project.getService(RadicleProjectApi.class);
        this.change = change;
        this.isLeft = isLeft;
        this.modifiedLines = calculateModifiedLines();
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

    private List<LineRange> calculateModifiedLines() {
        try {
            if (change.getBeforeRevision() == null && change.getAfterRevision() == null) {
                return List.of();
            }
            var beforeContent = change.getBeforeRevision() != null ? change.getBeforeRevision().getContent() : "";
            var afterContent = change.getAfterRevision() != null ? change.getAfterRevision().getContent() : "";
            var fragments = ComparisonManager.getInstance().compareLines(Strings.nullToEmpty(beforeContent), Strings.nullToEmpty(afterContent),
                    ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE);
            var lineRanges = new ArrayList<LineRange>();
            for (var fragment : fragments) {
                if (isLeft) {
                    lineRanges.add(new LineRange(fragment.getStartLine1(), fragment.getEndLine1()));
                } else {
                    lineRanges.add(new LineRange(fragment.getStartLine2(), fragment.getEndLine2()));
                }
            }
            return lineRanges;
        } catch (Exception e) {
            logger.warn("Unable to find modified lines");
            return List.of();
        }
    }

    public List<ThreadModel> getInlineComments(RadPatch patch) {
        var lastRevision = patch.getLatestRevision();
        var reviewComments = lastRevision.getReviewComments(getFilePath(), getCommitHash());
        var groupedDiscussions = new ArrayList<ThreadModel>();
        for (var disc : reviewComments) {
            var line = disc.location.start;
            if (!isCommentInModifiedLine(line)) {
                continue;
            }
            if (!disc.isReply()) {
                addNewThread(groupedDiscussions, disc);
            } else {
                addToExistingThread(groupedDiscussions, disc);
            }
        }
        return groupedDiscussions;
    }

    private void addNewThread(List<ThreadModel> groupedDiscussions, RadDiscussion discussion) {
        var newThreadModel = new ThreadModel(discussion.location.start, discussion);
        groupedDiscussions.add(newThreadModel);
    }

    private void addToExistingThread(List<ThreadModel> groupedDiscussions, RadDiscussion discussion) {
        var existingThreadModel = findThreadModel(groupedDiscussions, discussion);
        if (existingThreadModel.isPresent()) {
            var threadModel = existingThreadModel.get();
            threadModel.addRadDiscussion(discussion);
        } else {
            addNewThread(groupedDiscussions, discussion);
        }
    }

    private Optional<ThreadModel> findThreadModel(List<ThreadModel> groupedDiscussions, RadDiscussion discussion) {
        return groupedDiscussions.stream()
                .filter(threadModel -> threadModel.getRadDiscussion()
                        .stream()
                        .anyMatch(radDiscussion -> radDiscussion.id.equals(discussion.replyTo)))
                .findFirst();
    }


    private void updateEditorCommentsUi(RadPatch patch) {
        var fetched = this.api.fetchPatch(patch.radProject.id, patch.repo, patch.id);
        boolean success = fetched != null;
        if (success) {
            var inlineComments = getInlineComments(fetched);
            ApplicationManager.getApplication().invokeLater(() -> {
                update(inlineComments);
            });
        }
    }

    private boolean isCommentInModifiedLine(int line) {
        for (var modifiedLine : modifiedLines) {
            if (line >= modifiedLine.start && line <= modifiedLine.end) {
                return true;
            }
        }
        return false;
    }

    public String getCommitHash() {
        if (isLeft) {
            return change.getBeforeRevision().getRevisionNumber().asString();
        }
        return change.getAfterRevision().getRevisionNumber().asString();
    }

    public String getFilePath() {
        ContentRevision contentRevision;
        if (isLeft) {
            contentRevision = change.getBeforeRevision();
        } else {
            contentRevision = change.getAfterRevision();
        }
        var file = contentRevision.getFile();
        var repo = GitUtil.getRepositoryManager(api.getProject()).getRepositoryForFileQuick(file);
        var rootPath = repo.getRoot().getPath();
        return Paths.get(rootPath).relativize(Paths.get(file.getPath())).toString().replace("\\", "/");
    }

    public List<LineRange> getModifiedLines() {
        return modifiedLines;
    }

    public interface ChangeListener extends EventListener {
        void threadAdded(ThreadModel threadModel);
        void clearThreads();
    }
}
