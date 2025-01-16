package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;

public class RadCobShow extends RadAction {
    private static final Logger logger = Logger.getInstance(RadCobShow.class);

    private final String projectId;
    private final String objectId;
    private final RadCobList.Type type;

    public RadCobShow(GitRepository repo, String projectId, String objectId, RadCobList.Type type) {
        super(repo);
        this.projectId = projectId;
        this.objectId = objectId;
        this.type = type;
    }

    @Override
    public ProcessOutput run() {
        return rad.cobShow(repo, projectId, objectId, type);
    }

    public RadIssue getIssue() {
        var output = perform();
        var isSuccess = RadAction.isSuccess(output);
        if (!isSuccess) {
            return null;
        }
        var json = output.getStdout();
        RadIssue issue = null;
        try {
            issue = RadicleCliService.MAPPER.readValue(json, new TypeReference<>() { });
            tryResolveIssueAliases(issue);
            if (issue.discussion != null && !issue.discussion.isEmpty()) {
                var firstDiscussion = issue.discussion.getFirst();
                issue.author = firstDiscussion.author;
            }
            issue.project = repo.getProject();
            issue.repo = repo;
            issue.projectId = projectId;
            issue.id = objectId;
        } catch (Exception e) {
            logger.warn("Unable to deserialize issue:" + objectId + " in project:" + projectId, e);
        }
        return issue;
    }

    public RadPatch getPatch() {
        var output = perform();
        var isSuccess = RadAction.isSuccess(output);
        if (!isSuccess) {
            return null;
        }
        var json = output.getStdout();
        RadPatch patch = null;
        try {
            patch = RadicleCliService.MAPPER.readValue(json, new TypeReference<>() { });
            tryResolvePatchAliases(patch);
            return patch;
        } catch (Exception e) {
            logger.warn("Unable to deserialize patch from json: " + json, e);
        }
        return patch;
    }

    public void tryResolveIssueAliases(RadIssue issue) {
        if (issue.discussion != null && !issue.discussion.isEmpty()) {
            for (var disc : issue.discussion) {
                tryResolveDiscussionAliases(disc);
            }
        }
        if (issue.assignees != null && !issue.assignees.isEmpty()) {
            for (var as : issue.assignees) {
                as.tryResolveAlias(cli);
            }
        }
    }

    public void tryResolvePatchAliases(RadPatch patch) {
        if (patch.author != null) {
            patch.author.tryResolveAlias(cli);
        }
        if (patch.revisions != null) {
            for (var rev : patch.revisions.values()) {
                if (rev.author() != null) {
                    rev.author().tryResolveAlias(cli);
                }
                if (rev.edits() != null) {
                    for (var edit : rev.edits()) {
                        tryResolveEditAliases(edit);
                    }
                }
                if (rev.discussion() != null && rev.discussion().comments != null) {
                    for (var disc : rev.discussion().comments.values()) {
                        tryResolveDiscussionAliases(disc);
                    }
                }
            }
        }
    }

    public void tryResolveEditAliases(RadPatch.Edit edit) {
        if (edit == null) {
            return;
        }
        if (edit.author() != null) {
            edit.author().tryResolveAlias(cli);
        }
    }

    public void tryResolveDiscussionAliases(RadDiscussion disc) {
        if (disc == null) {
            return;
        }
        if (disc.author != null) {
            disc.author.tryResolveAlias(cli);
        }
        if (disc.edits != null) {
            for (var ed : disc.edits) {
                if (ed.author() != null) {
                    ed.author().tryResolveAlias(cli);
                }
            }
        }
        if (disc.reactions != null) {
            for (var react : disc.reactions) {
                for (var auth : react.authors()) {
                    auth.tryResolveAlias(cli);
                }
            }
        }
    }

    @Override
    public String getActionName() {
        return null;
    }
}
