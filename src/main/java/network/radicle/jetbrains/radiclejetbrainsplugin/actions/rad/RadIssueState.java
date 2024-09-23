package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadIssueState extends RadAction {
    private final String issueId;
    private final String state;

    public RadIssueState(GitRepository repo, String issueId, String state) {
        super(repo);
        this.issueId = issueId;
        this.state = state;
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.changeIssueState(repo, issueId, state);
    }

    @Override
    public String getActionName() {
        return "IssueState";
    }
}
