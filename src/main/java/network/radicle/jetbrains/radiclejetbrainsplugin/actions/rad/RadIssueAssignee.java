package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

import java.util.List;

public class RadIssueAssignee extends RadAction {
    private final String issueId;
    private final List<String> addedAssignees;
    private final List<String> deleteAssignees;

    public RadIssueAssignee(GitRepository repo, String issueId, List<String> addedAssignees, List<String> deleteAssignees) {
        super(repo);
        this.issueId = issueId;
        this.addedAssignees = addedAssignees;
        this.deleteAssignees = deleteAssignees;
    }


    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.addRemoveIssueAssignee(repo, issueId, addedAssignees, deleteAssignees);
    }

    @Override
    public String getActionName() {
        return "IssueAssignee";
    }
}
