package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;

import java.util.List;

public class RadIssueCreate extends RadAction {
    private final String title;
    private final String description;
    private final List<String> assignees;
    private final List<String> labels;

    public RadIssueCreate(GitRepository repo, String title, String description, List<String> assignees, List<String> labels) {
        super(repo);
        this.title = title;
        this.description = description;
        this.assignees = assignees;
        this.labels = labels;
    }

    @Override
    public ProcessOutput run() {
        return rad.createIssue(repo, title, description, assignees, labels);
    }

    @Override
    public String getActionName() {
        return null;
    }

    public boolean shouldUnlockIdentity() {
        return true;
    }
}
