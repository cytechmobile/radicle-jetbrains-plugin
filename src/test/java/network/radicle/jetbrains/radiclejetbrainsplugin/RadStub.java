package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.UsefulTestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RadStub extends RadicleApplicationService {
    public static final String ACTIVE_PROFILE = "myProfile";
    public final BlockingQueue<GeneralCommandLine> commands = new LinkedBlockingQueue<>();
    public static final String GIT_STORAGE = "Storage (git) /home/test/.local/xyz.radicle-link";
    public static final String KEYS_STORAGE = "Storage (keys) /home/test/.config/xyz.radicle-link";
    public static final String GIT_STORAGE_PATH = "/home/test/.local/xyz.radicle-link";
    public static final String KEYS_STORAGE_PATH = "/home/test/.config/xyz.radicle-link";

    @Override
    public ProcessOutput execAndGetOutputWithStdin(GeneralCommandLine cmdLine) {
        commands.add(cmdLine);
        var pr = new ProcessOutput(0);
        var stdout = "stdout";
        pr.appendStdout(stdout);
        return pr;
    }

    @Override
    public ProcessOutput execAndGetOutput(GeneralCommandLine cmdLine) throws ExecutionException {
        commands.add(cmdLine);
        var pr = new ProcessOutput(0);
        var stdout = "stdout";
        if (cmdLine.getCommandLineString().contains("--version")) {
            stdout = "rad 0.6.1";
        } else if (cmdLine.getCommandLineString().contains("which")) {
            stdout = "/usr/bin/rad";
        } else if (cmdLine.getCommandLineString().contains("self --profile")) {
            stdout = ACTIVE_PROFILE;
        } else if (cmdLine.getCommandLineString().contains("clone rad:ooo")) {
            return new ProcessOutput(-1);
        } else if (cmdLine.getCommandLineString().contains("self")) {
            stdout = GIT_STORAGE + "\n" + KEYS_STORAGE;
        }

        pr.appendStderr("stderr");
        pr.appendStdout(stdout);
        return pr;
    }

    public static RadStub replaceRadicleApplicationService(UsefulTestCase utc) {
        var stub = new RadStub();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(),
                RadicleApplicationService.class, stub, utc.getTestRootDisposable());
        return stub;
    }
}
