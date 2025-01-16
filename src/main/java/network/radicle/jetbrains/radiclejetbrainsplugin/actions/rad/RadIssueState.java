package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;

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
        return rad.changeIssueState(repo, issueId, state);
    }

    @Override
    public String getActionName() {
        return "IssueState";
    }
}
