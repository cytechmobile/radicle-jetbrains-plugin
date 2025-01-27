package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.execution.CommandLineUtil;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.UsefulTestCase;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushRepoResult;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadCobList;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsViewTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssueListPanelTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.TimelineTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.assertj.core.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT.RAD_HOME;
import static network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT.RAD_HOME1;
import static network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT.RAD_PATH;

public class RadStub extends RadicleProjectService {
    private static final Logger logger = LoggerFactory.getLogger(RadStub.class);

    public static final String SECOND_COMMIT_HASH = "970b7ceb6678bc42e4fb0b9e3628914e1e1b8dae";
    public static final String FIRST_PEER_ID = "hyy7y1g4bpkt9yn57wqph73dhfbwmnz9nf5hwtzdx8rhh3r1n7ibyw";
    public static final String SECOND_PEER_ID = "hyy8y1g4bpkt9yn57wqph73dhfbwmnz9nf5hwtzdx8rhh3r1n7ww";
    public static final String FIRST_BRANCH_NAME = "main";
    public static final String SECOND_BRANCH_NAME = "main2";
    public static final String TEMPLATE_TRACK_RESPONSE = "{exitCode=0, timeout=false, cancelled=false, stdout=asd " +
            "rad:git:hnrkqxsqc8oz8cth167tua786mof7btgrubto (pine.radicle.garden)\n" +
            "├── " + FIRST_PEER_ID + "\n" +
            "│   └── " + FIRST_BRANCH_NAME + " MY_COMMIT_HASH\n" +
            "│\n" +
            "├── " + SECOND_PEER_ID + "\n" +
            "│   └── " + SECOND_BRANCH_NAME + " " + SECOND_COMMIT_HASH + " \n" +
            "│\n" +
            ", stderr=}";
    public static final String SELF_KEYHASH = "SHA256:myFakeHash";
    public static final String SELF_DID =  "did:key:fakeDid";
    public static final String SELF_NODEID = "fakeDid";
    public static final String SELF_ALIAS = "alias";
    public static final RadAuthor SELF = new RadAuthor(SELF_DID, SELF_ALIAS);

    public final BlockingQueue<GeneralCommandLine> commands = new LinkedBlockingQueue<>();
    public final BlockingQueue<String> commandsStr = new LinkedBlockingQueue<>();
    public String firstCommitHash;
    private int counter = 0;
    public CommandOverride commandOverride;

    public RadStub(String commitHash, Project project) {
        super(project);
        this.firstCommitHash = commitHash;
    }

    @Override
    public ProcessOutput executeCommandFromFile(String exePath, GitRepository repo, List<String> params) {
        var command = exePath + " " + String.join(" ", params);
        commandsStr.add(command);
        var pr = new ProcessOutput(0);
        var stdout = "stdout";
        pr.appendStdout(stdout);
        if (exePath.equals("git")) {
            pr.appendStderr("t t 123123 r");
        }
        return pr;
    }

    @Override
    public ProcessOutput executeCommandFromFile(GitRepository repo, List<String> params) {
        var command = String.join(" ", params);
        commandsStr.add(command);
        var pr = new ProcessOutput(0);
        if (command.contains("comment") && command.contains(CommandLineUtil.posixQuote("break"))) {
            pr = new ProcessOutput(-1);
        }
        var stdout = "stdout";
        pr.appendStdout(stdout);
        return pr;
    }

    @Override
    public ProcessOutput execAndGetOutputWithStdin(GeneralCommandLine cmdLine, String stdin) {
        cmdLine.withEnvironment("stdin", stdin);
        commands.add(cmdLine);
        var pr = new ProcessOutput(0);
        var stdout = "stdout";
        pr.appendStdout(stdout);
        return pr;
    }

    @Override
    public ProcessOutput execAndGetOutput(GeneralCommandLine cmdLine) {
        if (commandOverride != null) {
            var result = commandOverride.exec(cmdLine);
            if (result != null) {
                return result;
            }
        }
        commands.add(cmdLine);
        var pr = new ProcessOutput(0);
        var stdout = "stdout";
        if (cmdLine.getCommandLineString().contains("--version")) {
            stdout = "rad " + AbstractIT.RAD_VERSION;
        } else if (cmdLine.getCommandLineString().contains("path")) {
            stdout = RAD_HOME;
        } else if (cmdLine.getCommandLineString().contains("cob list") && cmdLine.getCommandLineString().contains(RadCobList.Type.PATCH.value)) {
            StringBuilder patchesId = new StringBuilder();
            for (var patch : PatchListPanelTest.getTestPatches()) {
                patchesId.append(patch.id).append("\n");
            }
            stdout = patchesId.toString();
        } else if (cmdLine.getCommandLineString().contains("cob show") && cmdLine.getCommandLineString().contains(RadCobList.Type.PATCH.value)) {
            var parts = cmdLine.getCommandLineString().split("--object");
            var patchId = parts[parts.length - 1].replace("\"", "").trim();
            var patches = new ArrayList<>(PatchListPanelTest.getTestPatches());
            if (TimelineTest.patch != null) {
                var myPatch = new RadPatch(TimelineTest.patch);
                myPatch.repo = null;
                myPatch.project = null;
                patches.add(myPatch);
            }
            var patch = patches.stream().filter(p -> p.id.equals(patchId)).findFirst().orElse(null);
            try {
                stdout = RadicleCliService.MAPPER.writeValueAsString(patch);
            } catch (Exception e) {
                logger.warn("unable to write values as string", e);
            }
        } else if (cmdLine.getCommandLineString().contains("cob list") && cmdLine.getCommandLineString().contains(RadCobList.Type.ISSUE.value)) {
            StringBuilder issuesId = new StringBuilder();
            for (var issue : IssueListPanelTest.getTestIssues()) {
                issuesId.append(issue.id).append("\n");
            }
            stdout = issuesId.toString();
        } else if (cmdLine.getCommandLineString().contains("cob show") && cmdLine.getCommandLineString().contains(RadCobList.Type.ISSUE.value)) {
            var parts = cmdLine.getCommandLineString().split("--object");
            var issueId = parts[parts.length - 1].replace("\"", "").trim();
            var issues = IssueListPanelTest.getTestIssues();
            var issue = issues.stream().filter(is -> issueId.equals(is.id)).findFirst().orElse(null);
            if (issue != null) {
                var serializeIssue = RadicleCliService.MAPPER.convertValue(issue, new TypeReference<Map<String, Object>>() { });
                var discussions = (ArrayList<Map<String, Object>>) serializeIssue.get("thread");
                serializeIssue.remove("thread");
                var discussionMap = new HashMap<String, Object>();
                for (var disc : discussions) {
                    discussionMap.put(disc.get("id").toString(), disc);
                }
                var commentMap = new HashMap<String, Object>();
                commentMap.put("comments", discussionMap);
                serializeIssue.put("thread", commentMap);
                if (issue != null) {
                    try {
                        stdout = RadicleCliService.MAPPER.writeValueAsString(serializeIssue);
                    } catch (Exception e) {
                        logger.warn("Unable to write value as a string");
                    }
                }
            }
        } else if (cmdLine.getCommandLineString().contains("which")) {
            stdout = RAD_PATH;
        } else if (cmdLine.getCommandLineString().contains("clone rad:ooo")) {
            return new ProcessOutput(-1);
        } else if (cmdLine.getCommandLineString().contains("ssh-add")) {
            stdout = SELF_KEYHASH;
        } else if (cmdLine.getCommandLineString().contains("self --alias")) {
            stdout = SELF_ALIAS + "\n";
        } else if (cmdLine.getCommandLineString().contains("self --nid")) {
            stdout = SELF_NODEID + "\n";
        } else if (cmdLine.getCommandLineString().contains("self --did")) {
            stdout = SELF_DID + "\n";
        } else if (cmdLine.getCommandLineString().contains("self --ssh-fingerprint")) {
            var envRadHome = cmdLine.getEnvironment().get("RAD_HOME");
            if (cmdLine.getCommandLineString().contains(RadicleSettingsViewTest.NEW_RAD_INSTALLATION) || (!Strings.isNullOrEmpty(envRadHome) &&
                    envRadHome.contains(RadicleSettingsViewTest.NEW_RAD_INSTALLATION))) {
                counter++;
            }
            if (cmdLine.getCommandLineString().contains(RAD_HOME1) || (!Strings.isNullOrEmpty(envRadHome) &&
                    envRadHome.contains(RAD_HOME1))) {
                stdout = SELF_KEYHASH + "A" + "\n";
            } else if (cmdLine.getCommandLineString().contains(RAD_HOME) || (!Strings.isNullOrEmpty(envRadHome) &&
                    envRadHome.contains(RAD_HOME))) {
                stdout = SELF_KEYHASH + "\n";
            } else if ((cmdLine.getCommandLineString().contains(RadicleSettingsViewTest.NEW_RAD_INSTALLATION) || (!Strings.isNullOrEmpty(envRadHome) &&
                    envRadHome.contains(RadicleSettingsViewTest.NEW_RAD_INSTALLATION))) && counter == 2) {
                stdout = SELF_KEYHASH + "\n";
            } else {
                pr.setExitCode(-1);
            }
        } else if (cmdLine.getCommandLineString().contains("inspect")) {
            if (cmdLine.getCommandLineString().contains("--identity")) {
                stdout = "{\"payload\": {" +
                         "\"xyz.radicle.project\": {" +
                         "\"defaultBranch\": \"main\"," +
                         "\"description\": \"test project\"," +
                         "\"name\": \"test\"" +
                         "}" +
                         "}," +
                         " \"delegates\": [\"" + SELF_DID + "\"]," +
                         "\"threshold\": 1" +
                         "}";
            } else if (cmdLine.getCommandLineString().contains("--delegates")) {
                stdout = SELF_DID + " (" + SELF_ALIAS + ")";
            } else {
                stdout = "rad:123";
            }
        } else if (cmdLine.getCommandLineString().contains("track")) {
            stdout = TEMPLATE_TRACK_RESPONSE.replace("MY_COMMIT_HASH", this.firstCommitHash);
        } else if (cmdLine.getCommandLineString().contains("remote ls")) {
            stdout = "";
        }

        pr.appendStderr("stderr");
        pr.appendStdout(stdout);
        return pr;
    }

    @Override
    public ProcessOutput setUpstream(String branch, String targetBranch, GitRepository repository) {
        return new ProcessOutput(0);
    }

    @Override
    public boolean isSuccessPush(GitPushRepoResult gitPushRepoResult) {
        return true;
    }

    @Override
    public String getBranchRevision(Project project, GitRepository repo, String branchName) {
        return repo.getCurrentRevision();
    }

    @Override
    public Map<String, Object> pushChanges(GitRepository gitRepository, GitLocalBranch gitLocalBranch, GitRemote gitRemote) {
        return Map.of("success", true, "message", "");
    }

    public ProcessOutput fetchPeerChanges(GitRepository repository) {
        return new ProcessOutput(0);
    }

    public static RadStub replaceRadicleProjectService(UsefulTestCase utc, String commitHash, Project project) {
        var stub = new RadStub(commitHash, project);
        ServiceContainerUtil.replaceService(project, RadicleProjectService.class, stub, utc.getTestRootDisposable());
        return stub;
    }

    public interface CommandOverride {
        ProcessOutput exec(GeneralCommandLine cmd);
    }
}
