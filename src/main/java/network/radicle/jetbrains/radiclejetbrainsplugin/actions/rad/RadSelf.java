package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadSelf extends RadAction {
    private final String radHome;
    private final String radPath;

    public RadSelf(String radHome, String radPath) {
        this.radHome = radHome;
        this.radPath = radPath;
    }

    @Override
    public ProcessOutput run() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.self(radHome, radPath);
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String getActionName() {
        return "Self";
    }
}
