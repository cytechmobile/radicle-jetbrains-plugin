package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class RadSelf implements RadAction {
    private static final Logger logger = LoggerFactory.getLogger(RadSelf.class);
    private RadSelf.RadSelfAction action;
    private String errorMsg = "";

    public enum RadSelfAction {
        ACTIVE_PROFILE, GET_PROFILES
    }

    public RadSelf(RadSelfAction action) {
        this.action = action;
        setMessages();
    }

    private void setMessages() {
        if (action == RadSelfAction.ACTIVE_PROFILE) {
            errorMsg = RadicleBundle.message("activeProfileError");
        } else if (action == RadSelfAction.GET_PROFILES) {
            errorMsg = RadicleBundle.message("profilesError");
        }
    }

    public ProcessOutput activeProfile() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.self(true);
    }

    private ProcessOutput getProfiles() {
        var storage = BasicAction.getStoragePath();
        if (storage == null || Strings.isNullOrEmpty(storage.keysStoragePath)) {
            return new ProcessOutput(-1);
        }
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.executeCommand("ls", ".", List.of(storage.keysStoragePath), null, false);
    }

    @Override
    public ProcessOutput run() {
        switch (action) {
            case ACTIVE_PROFILE:
                return activeProfile();
            case GET_PROFILES:
                var output = getProfiles();
                if (output.getExitCode() == 0) {
                    var profiles = output.getStdoutLines(true).stream().filter(p -> p.contains("-"))
                            .collect(Collectors.toList());
                    output = new ProcessOutput(0);
                    output.appendStdout(String.join(",",profiles));
                }
                return output;
            default:
                return new ProcessOutput(-1);
        }
    }

    @Override
    public String getErrorMessage() {
        return errorMsg;
    }

    @Override
    public String getSuccessMessage() {
        return "";
    }

    @Override
    public String getNotificationSuccessMessage() {
        return "";
    }

    @Override
    public GitRepository getRepo() {
        return null;
    }
}
