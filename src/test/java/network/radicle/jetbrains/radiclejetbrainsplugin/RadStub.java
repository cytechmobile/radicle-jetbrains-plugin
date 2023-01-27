package network.radicle.jetbrains.radiclejetbrainsplugin;

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

    public String firstCommitHash;
    public static final String SECOND_COMMIT_HASH = "970b7ceb6678bc42e4fb0b9e3628914e1e1b8dae";
    public static final String FIRST_PEER_ID = "hyy7y1g4bpkt9yn57wqph73dhfbwmnz9nf5hwtzdx8rhh3r1n7ibyw";
    public static final String SECOND_PEER_ID = "hyy8y1g4bpkt9yn57wqph73dhfbwmnz9nf5hwtzdx8rhh3r1n7ww";
    public static final String FIRST_BRANCH_NAME = "master";
    public static final String SECOND_BRANCH_NAME = "master2";
    public String trackResponse = "{exitCode=0, timeout=false, cancelled=false, stdout=asd " +
            "rad:git:hnrkqxsqc8oz8cth167tua786mof7btgrubto (pine.radicle.garden)\n" +
            "├── " + FIRST_PEER_ID + "\n" +
            "│   └── " + FIRST_BRANCH_NAME + " MY_COMMIT_HASH\n" +
            "│\n" +
            "├── " + SECOND_PEER_ID + "\n" +
            "│   └── " + SECOND_BRANCH_NAME + " " + SECOND_COMMIT_HASH + " \n" +
            "│\n" +
            ", stderr=}";

    public RadStub(String commitHash) {
       this.firstCommitHash = commitHash;
    }

    @Override
    public ProcessOutput execAndGetOutputWithStdin(GeneralCommandLine cmdLine) {
        commands.add(cmdLine);
        var pr = new ProcessOutput(0);
        var stdout = "stdout";
        pr.appendStdout(stdout);
        return pr;
    }

    @Override
    public ProcessOutput execAndGetOutput(GeneralCommandLine cmdLine) {
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
        } else if (cmdLine.getCommandLineString().contains("track")) {
            trackResponse = trackResponse.replace("MY_COMMIT_HASH", this.firstCommitHash);
            stdout = trackResponse;
        } else if (cmdLine.getCommandLineString().contains("remote ls")) {
            stdout = "";
        }

        pr.appendStderr("stderr");
        pr.appendStdout(stdout);
        return pr;
    }

    public static RadStub replaceRadicleApplicationService(UsefulTestCase utc, String commitHash) {
        var stub = new RadStub(commitHash);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(),
                RadicleApplicationService.class, stub, utc.getTestRootDisposable());
        return stub;
    }
}
