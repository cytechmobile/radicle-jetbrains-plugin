package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Supplier;

public class RadPush implements RadAction {
    private static final Logger logger = LoggerFactory.getLogger(RadPush.class);
    private final GitRepository repo;
    private final String seed;
    private static final String RAD_UI = "https://app.radicle.xyz/seeds";

    public RadPush(GitRepository repo, String seed) {
        this.repo = repo;
        this.seed = seed;
    }

    @Override
    public ProcessOutput run () {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.push(repo,seed);
    }

    @Override
    public String getErrorMessage() {
        return RadicleBundle.message("errorInRadPush");
    }

    @Override
    public String getSuccessMessage() {
        return RadicleBundle.message("successInRadPush");
    }

    @Override
    public String getNotificationSuccessMessage() {
        return RadicleBundle.message("radPushNotification");
    }

    @Override
    public GitRepository getRepo() {
        return repo;
    }

    @Override
    public List<NotificationAction> notificationActions() {
       return List.of(new PushNotificationAction(seed, repo, RadicleBundle.lazyMessage("Open")));
    }

    public static class PushNotificationAction extends NotificationAction {
        private final GitRepository repo;
        private final String host;

        public PushNotificationAction(String host, GitRepository repo, Supplier<String> msg) {
            super(msg);
            this.repo = repo;
            this.host = host;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
                var output = rad.inspect(repo);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (output.getExitCode() == 0) {
                        var projectIdentity = (output.getStdout()).replace("\n","");
                        var url = RAD_UI + "/" + host + "/" + projectIdentity;
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            try {
                                Desktop.getDesktop().browse(new URI(url));
                            } catch (IOException | URISyntaxException ex) {
                                logger.warn("unable to open browser",ex);
                            }
                        }
                    }
                }, ModalityState.any());
            });
        }
    }
}
