package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationAction;
import git4idea.repo.GitRepository;

import java.util.List;

public interface RadAction {
    ProcessOutput run();
    String getErrorMessage();
    String getSuccessMessage();
    String getNotificationSuccessMessage();
    GitRepository getRepo();
    List<NotificationAction> notificationActions();
}
