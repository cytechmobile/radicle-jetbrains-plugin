package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

import java.util.List;

public class RadAuth extends RadAction {
    private final String name;
    private final String passphrase;
    private final RadAuthAction action;

    public RadAuth(String name, String passphrase, RadAuthAction action) {
        this.name = name;
        this.passphrase = passphrase;
        this.action = action;
    }

    @Override
    public String getActionName() {
        if (action == RadAuthAction.CREATE_IDENTITY) {
            return "AuthCreateIdentity";
        } else if (action == RadAuthAction.SET_DEFAULT_IDENTITY) {
            return "AuthSetDefaultIdentity";
        } else if (action == RadAuthAction.REMOVE_IDENTITY) {
            return "AuthRemoveIdentity";
        }
        return "";
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

    public ProcessOutput removeIdentity() {
        var storagePath = getStoragePath();
        if (Strings.isNullOrEmpty(storagePath.keysStoragePath) || Strings.isNullOrEmpty(storagePath.gitStoragePath)) {
            return new ProcessOutput(-1);
        }
        var gitStoragePath = storagePath.gitStoragePath + "/" + name;
        var keysStoragePath = storagePath.keysStoragePath + "/" + name;
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.executeCommand("rm", ".", List.of("-rf", gitStoragePath, keysStoragePath), null, false);
    }

    public ProcessOutput setDefaultIdentity() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.auth(name, passphrase, true);
    }

    public ProcessOutput createNewIdentity() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.auth(name, passphrase, false);
    }

    public enum RadAuthAction {
        CREATE_IDENTITY, REMOVE_IDENTITY, SET_DEFAULT_IDENTITY
    }
}
