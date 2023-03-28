package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadFetch extends RadAction {
    public RadFetch(GitRepository repo) {
        super(repo);
    }

    @Override
    public String getActionName() {
        return "Fetch";
    }

    @Override
    public ProcessOutput run() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        var output = rad.fetch(repo);
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
