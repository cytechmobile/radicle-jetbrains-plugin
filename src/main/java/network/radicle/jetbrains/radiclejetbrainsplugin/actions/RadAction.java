package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;

public interface RadAction {
    ProcessOutput run(GitRepository repo);
    String getErrorMessage();
    String getSuccessMessage();
    String getNotificationSuccessMessage();
}
