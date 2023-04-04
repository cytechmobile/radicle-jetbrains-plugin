package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.UsefulTestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.assertj.core.util.Strings;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT.RAD_HOME;
import static network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT.RAD_HOME1;
import static network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT.RAD_PATH;

public class RadStub extends RadicleProjectService {
    public final BlockingQueue<GeneralCommandLine> commands = new LinkedBlockingQueue<>();
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
    public String keyHash = "SHA256:myFakeHash";
    public static String nodeId =  "did:key:fakeDid";
    public String radSelfResponse = "ID             " + nodeId + "\n" +
            "Node ID        fakeDid\n" +
            "Key (hash)     " + keyHash + "\n" +
            "Key (full)     ssh-ed25519 myFullKey\n" +
            "Storage (git)  /home/test/.myInstallation/storage\n" +
            "Storage (keys) /home/test/.myInstallation/keys\n" +
            "Node (socket)  /home/test/.myInstallation/node/radicle.sock";


    public RadStub(String commitHash, Project project) {
        super(project);
        this.firstCommitHash = commitHash;
    }

    @Override
    public ProcessOutput execAndGetOutputWithStdin(GeneralCommandLine cmdLine, String stdin) {
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
        } else if (cmdLine.getCommandLineString().contains("path")) {
            stdout = RAD_HOME;
        } else if (cmdLine.getCommandLineString().contains("which")) {
            stdout = RAD_PATH;
        } else if (cmdLine.getCommandLineString().contains("clone rad:ooo")) {
            return new ProcessOutput(-1);
        } else if (cmdLine.getCommandLineString().contains("ssh-add")) {
            stdout = keyHash;
        } else if (cmdLine.getCommandLineString().contains("self")) {
            var envRadHome = cmdLine.getEnvironment().get("RAD_HOME");
            if (cmdLine.getCommandLineString().contains(RAD_HOME) || (!Strings.isNullOrEmpty(envRadHome) &&
                    envRadHome.contains(RAD_HOME))) {
                stdout = radSelfResponse;
            } else if (cmdLine.getCommandLineString().contains(RAD_HOME1) || (!Strings.isNullOrEmpty(envRadHome) &&
                    envRadHome.contains(RAD_HOME1))) {
                keyHash = keyHash + "A";
                stdout = trackResponse;
            } else {
                pr.setExitCode(-1);
            }
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

    public static RadStub replaceRadicleProjectService(UsefulTestCase utc, String commitHash, Project project) {
        var stub = new RadStub(commitHash, project);
        ServiceContainerUtil.replaceService(project,
                RadicleProjectService.class, stub, utc.getTestRootDisposable());
        return stub;
    }
}
