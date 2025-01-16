package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;

public class RadCheckout extends RadAction {
    private String patchId;

    public RadCheckout(GitRepository repo, String patchId) {
        super(repo);
        this.patchId = patchId;
    }

    @Override
    public ProcessOutput run() {
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
