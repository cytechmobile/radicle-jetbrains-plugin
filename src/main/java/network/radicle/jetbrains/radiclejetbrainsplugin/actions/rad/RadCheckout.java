package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadCheckout extends RadAction {
    private String patchId;

    public RadCheckout(GitRepository repo, String patchId) {
        super(repo);
        this.patchId = patchId;
    }

    @Override
    public ProcessOutput run() {
        var rad = repo.getProject().getService(RadicleProjectService.class);
        return rad.checkout(repo, patchId);
    }

    @Override
    public boolean shouldShowNotification() {
        return false;
    }

    @Override
    public String getActionName() {
        return "Checkout";
    }
}
