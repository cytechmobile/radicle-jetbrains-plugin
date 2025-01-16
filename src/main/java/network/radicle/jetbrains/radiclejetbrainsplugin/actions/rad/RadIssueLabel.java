package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;

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
        return rad.addRemoveIssueLabels(repo, issueId, addedLabels, deleteLabels);
    }

    @Override
    public String getActionName() {
        return "Label";
    }
}
