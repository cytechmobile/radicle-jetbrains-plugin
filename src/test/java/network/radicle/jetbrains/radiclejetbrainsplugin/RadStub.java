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
    public final BlockingQueue<GeneralCommandLine> commands = new LinkedBlockingQueue<>();

    @Override
    public ProcessOutput execAndGetOutput(GeneralCommandLine cmdLine) throws ExecutionException {
        commands.add(cmdLine);
        var stdout = "stdout";
        if (cmdLine.getCommandLineString().contains("--version")) {
            stdout = "rad 0.6.0";
        }
        return new ProcessOutput(stdout, "stderr", 0, false, false);
    }

    public static RadStub replaceRadicleApplicationService(UsefulTestCase utc) {
        var stub = new RadStub();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(),
                RadicleApplicationService.class, stub, utc.getTestRootDisposable());
        return stub;
    }
}
