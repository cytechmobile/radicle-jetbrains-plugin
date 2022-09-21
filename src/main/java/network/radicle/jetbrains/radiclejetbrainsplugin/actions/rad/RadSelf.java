package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    }

    public ProcessOutput activeProfile() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        errorMsg = RadicleBundle.message("activeProfileError");
        return rad.self(true);
    }

    private String getConfigPath() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        try {
            var output = rad.self(false);
            if (output.getExitCode() == 0) {
                List<String> list = output.getStdoutLines(true);
                var path = list.get(list.size() - 1);
                String pattern = "[*/](.*?)radicle-link";
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(path);
                if (m.find()) {
                    return m.group();
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to get radicle config path");
        }
        return "";
    }

    private ProcessOutput getProfiles() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        var output = rad.executeCommand("", ".", List.of("ls", getConfigPath()), null);
        errorMsg = RadicleBundle.message("profilesError");
        return output;
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
