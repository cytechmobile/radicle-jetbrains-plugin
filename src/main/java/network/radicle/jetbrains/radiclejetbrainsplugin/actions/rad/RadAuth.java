package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadAuth extends RadAction {
    private final String passphrase;
    private final String radHome;
    private final String radPath;
    private final RadAuthAction action;

    public RadAuth(String passphrase, String radHome, String radPath, RadAuthAction action) {
        this.passphrase = passphrase;
        this.action = action;
        this.radHome = radHome;
        this.radPath = radPath;
    }

    @Override
    public boolean shouldShowNotification() {
        return false;
    }

    @Override
    public String getActionName() {
        if (action == RadAuthAction.CREATE_IDENTITY) {
            return "AuthCreateIdentity";
        } else if (action == RadAuthAction.UNLOCKED_IDENTITY) {
            return "AuthUnlockedIdentity";
        }
        return "";
    }

    @Override
    public ProcessOutput run() {
        switch (action) {
            case UNLOCKED_IDENTITY:
            case CREATE_IDENTITY:
                return createNewIdentity();
            default:
                return new ProcessOutput(-1);
        }
    }

    public ProcessOutput createNewIdentity() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        var output = rad.auth(passphrase, radHome, radPath);
        /* rad auth return success exit code (0) and a failed msg if the password is wrong */
        var isSuccess = RadAuth.isSuccess(output) && !output.getStdout().contains("failed");
        var pr = new ProcessOutput(isSuccess ? 0 : -1);
        /* Write from stdOut to stdErr in order to appear the message in the notification */
        var stdOut = output.getStdout();
        var errorMessage = !Strings.isNullOrEmpty(stdOut) ? stdOut : RadicleBundle.message("radCliError");
        pr.appendStderr(errorMessage);
        return pr;
    }

    public enum RadAuthAction {
        CREATE_IDENTITY, UNLOCKED_IDENTITY
    }
}
