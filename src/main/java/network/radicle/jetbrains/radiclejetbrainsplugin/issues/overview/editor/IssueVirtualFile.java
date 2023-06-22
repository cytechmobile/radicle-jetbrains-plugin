package network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview.editor;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.testFramework.LightVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import org.jetbrains.annotations.NotNull;

public class IssueVirtualFile extends LightVirtualFile {
    private final RadIssue issue;
    private final SingleValueModel<RadIssue> issueModel;

    public IssueVirtualFile(SingleValueModel<RadIssue> issueModel) {
        this.issue = issueModel.getValue();
        this.issueModel = issueModel;
    }

    @Override
    public @NlsSafe @NotNull String getName() {
        return issue.id;
    }

    @Override
    public @NotNull @NlsSafe String getPresentableName() {
        return issue.title;
    }

    public RadIssue getIssue() {
        return issue;
    }

    public SingleValueModel<RadIssue> getIssueModel() {
        return issueModel;
    }

}
