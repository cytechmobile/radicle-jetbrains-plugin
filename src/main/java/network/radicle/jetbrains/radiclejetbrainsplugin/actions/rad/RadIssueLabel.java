package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

import java.util.List;

public class RadIssueLabel extends RadAction {
    private final String issueId;
    private final List<String> addedLabels;
    private final List<String> deleteLabels;

    public RadIssueLabel(GitRepository repo, String issueId, List<String> addedLabels, List<String> deleteLabels) {
        super(repo);
        this.issueId = issueId;
        this.addedLabels = addedLabels;
        this.deleteLabels = deleteLabels;
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.addRemoveIssueLabels(repo, issueId, addedLabels, deleteLabels);
    }

    @Override
    public String getActionName() {
        return "Label";
    }
}
