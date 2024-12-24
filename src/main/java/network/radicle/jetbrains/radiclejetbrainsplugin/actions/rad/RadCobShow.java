package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

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
        var rad = project.getService(RadicleProjectService.class);
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
            var firstDiscussion = issue.discussion.getFirst();
            issue.author = firstDiscussion.author;
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
            return patch;
        } catch (Exception e) {
            logger.warn("Unable to deserialize patch from json: " + json, e);
        }
        return patch;
    }

    @Override
    public String getActionName() {
        return null;
    }
}
