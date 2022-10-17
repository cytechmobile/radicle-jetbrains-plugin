package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

import java.util.List;

public class RadAuth implements RadAction {

    private String notificationSuccessMsg;
    private String errorMsg;
    private String successMsg;
    private String name;
    private String passphrase;
    private RadAuthAction action;

    public RadAuth (String name, String passphrase, RadAuthAction action) {
        this.name = name;
        this.passphrase = passphrase;
        this.action = action;
        setMessages();
    }

    private void setMessages() {
        if (action == RadAuthAction.CREATE_IDENTITY) {
            successMsg = RadicleBundle.message("createIdentitySuccess");
            notificationSuccessMsg = RadicleBundle.message("createIdentitySuccess");
            errorMsg = RadicleBundle.message("createIdentityError");
        } else if (action == RadAuthAction.SET_DEFAULT_IDENTITY) {
            successMsg = RadicleBundle.message("setDefaultIdentitySuccess");
            notificationSuccessMsg =  RadicleBundle.message("setDefaultIdentitySuccess");
            errorMsg = RadicleBundle.message("setDefaultIdentityError");
        } else if (action == RadAuthAction.REMOVE_IDENTITY) {
            successMsg = RadicleBundle.message("removeIdentitySuccess");
            notificationSuccessMsg = RadicleBundle.message("removeIdentitySuccess");
            errorMsg = RadicleBundle.message("removeIdentityError");
        }
    }

    public enum RadAuthAction {
        CREATE_IDENTITY, REMOVE_IDENTITY, SET_DEFAULT_IDENTITY
    }

    public ProcessOutput removeIdentity() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.removeIdentity(name);
    }

    public ProcessOutput setDefaultIdentity() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        var output =  rad.auth(name,passphrase,true);
        var msg = output.getStderr();
        if (msg.contains("Adding your radicle key to ssh-agent")) {
             output.setExitCode(0);
             return output;
        }
        return output;
    }

    public  ProcessOutput createNewIdentity() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.auth(name,passphrase,false);
    }

    @Override
    public ProcessOutput run() {
        switch (action) {
            case CREATE_IDENTITY:
                return createNewIdentity();
            case REMOVE_IDENTITY:
                return removeIdentity();
            case SET_DEFAULT_IDENTITY:
                return setDefaultIdentity();
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
        return successMsg;
    }

    @Override
    public String getNotificationSuccessMessage() {
        return notificationSuccessMsg;
    }

    @Override
    public GitRepository getRepo() {
        return null;
    }

    @Override
    public List<NotificationAction> notificationActions() {
        return null;
    }
}
