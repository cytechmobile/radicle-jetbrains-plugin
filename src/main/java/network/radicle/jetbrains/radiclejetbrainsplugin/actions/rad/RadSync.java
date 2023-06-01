package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadSync extends RadAction {
    public RadSync(GitRepository repo) {
        super(repo);
    }

    @Override
    public String getActionName() {
        return "Sync";
    }

    @Override
    public ProcessOutput run() {
        var rad = repo.getProject().getService(RadicleProjectService.class);
        var output = rad.sync(repo);
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
