package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;

public interface RadAction {
    ProcessOutput run();
    String getErrorMessage();
    String getSuccessMessage();
    String getNotificationSuccessMessage();
    GitRepository getRepo();
}
