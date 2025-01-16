package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;

public class RadSync extends RadAction {
    private boolean fetch;

    public RadSync(GitRepository repo, boolean fetch) {
        super(repo);
        this.fetch = fetch;
    }

    @Override
    public String getActionName() {
        if (this.fetch) {
            return "Sync_Fetch";
        }
        return "Sync";
    }

    @Override
    public ProcessOutput run() {
        var output = rad.sync(repo, this.fetch);
        /* rad fetch return success exit code (0) and a failed msg if fetch command failed */
        var isSuccess = RadAction.isSuccess(output) && !output.getStdout().contains("Failed");
        var pr = new ProcessOutput(isSuccess ? 0 : -1);
        /* Write from stdOut to stdErr in order to appear the message in the notification */
        var stdOut = output.getStdout();
        var errorMessage = !Strings.isNullOrEmpty(stdOut) ? stdOut : RadicleBundle.message("radCliError");
        pr.appendStderr(errorMessage);
        return pr;
    }
}
