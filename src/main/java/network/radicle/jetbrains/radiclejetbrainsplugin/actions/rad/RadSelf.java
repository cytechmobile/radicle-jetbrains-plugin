package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class RadSelf extends RadAction {
    private RadSelfAction action;

    public RadSelf(RadSelfAction action) {
        this.action = action;
    }

    @Override
    public String getActionName() {
        if (action == RadSelfAction.ACTIVE_PROFILE) {
            return "SelfActiveProfile";
        } else if (action == RadSelfAction.GET_PROFILES) {
            return "SelfGetProfiles";
        }
        return "";
    }

    public ProcessOutput activeProfile() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.self(true);
    }

    private ProcessOutput getProfiles() {
        var storage = getStoragePath();
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

    public enum RadSelfAction {
        ACTIVE_PROFILE, GET_PROFILES
    }
}
