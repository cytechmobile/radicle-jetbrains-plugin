package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadCobShow extends RadAction {
    private static final Logger logger = LoggerFactory.getLogger(RadCobShow.class);

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
            var firstDiscussion = issue.discussion.get(0);
            issue.author = firstDiscussion.author;
            issue.project = repo.getProject();
            issue.repo = repo;
            issue.projectId = projectId;
            issue.id = objectId;
        } catch (Exception e) {
            logger.warn("Unable to deserialize issue");
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
            logger.warn("Unable to deserialize patch");
        }
        return patch;
    }

    @Override
    public String getActionName() {
        return null;
    }
}
