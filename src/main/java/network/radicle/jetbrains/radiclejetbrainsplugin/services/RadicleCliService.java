package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadCobList;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadCobShow;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;

import java.util.ArrayList;
import java.util.List;

public class RadicleCliService {
    private final RadicleProjectSettingsHandler projectSettingsHandler;
    private Project project;

    public RadicleCliService(Project project) {
        this(new RadicleProjectSettingsHandler(project));
        this.project = project;
    }

    public RadicleCliService(RadicleProjectSettingsHandler radicleProjectSettingsHandler) {
        this.projectSettingsHandler = radicleProjectSettingsHandler;
    }

    public RadIssue getIssue(GitRepository repo, String projectId, String objectId) {
        var cobShow = new RadCobShow(repo, projectId, objectId, RadCobList.Type.ISSUE);
        return cobShow.getIssue();
    }

    public List<RadIssue> getIssues(GitRepository repo, String projectId) {
        var cobList = new RadCobList(repo, projectId, RadCobList.Type.ISSUE);
        var listOutput = cobList.perform();
        var listOutputSuccess = RadAction.isSuccess(listOutput);
        if (!listOutputSuccess) {
            return List.of();
        }
        var issueIds = listOutput.getStdoutLines();
        var issues = new ArrayList<RadIssue>();
        for (var objectId : issueIds) {
            var issue = getIssue(repo, projectId, objectId);
            if (issue == null) {
                continue;
            }
            issues.add(issue);
        }
        return issues;
    }
}
