package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.google.common.base.Strings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.serviceContainer.NonInjectable;
import git4idea.fetch.GitFetchSupport;
import git4idea.repo.GitRepository;
import git4idea.util.GitVcsConsoleWriter;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RadicleProjectService {
    private static final Logger logger = LoggerFactory.getLogger(RadicleProjectService.class);
    private static final int TIMEOUT = 60_000;
    private final RadicleProjectSettingsHandler projectSettingsHandler;

    public RadicleProjectService(Project project) {
        this(new RadicleProjectSettingsHandler(project));
    }

    @NonInjectable
    public RadicleProjectService(RadicleProjectSettingsHandler radicleProjectSettingsHandler) {
        this.projectSettingsHandler = radicleProjectSettingsHandler;
    }

    public ProcessOutput homePath(String radPath) {
        return executeCommand(radPath, ".", List.of("path"), null);
    }

    public ProcessOutput radPath() {
        return executeCommand("which", ".", List.of("rad"), null);
    }

    public ProcessOutput self(String radHome, String radPath) {
        var alias = executeCommand(radPath, radHome, ".", List.of("self", "--alias"), null, "");
        var nid = executeCommand(radPath, radHome, ".", List.of("self", "--nid"), null, "");
        var did = executeCommand(radPath, radHome, ".", List.of("self", "--did"), null, "");
        var sshFingerPrint = executeCommand(radPath, radHome, ".", List.of("self", "--ssh-fingerprint"), null, "");
        if (!RadAction.isSuccess(alias) || !RadAction.isSuccess(nid) || !RadAction.isSuccess(did) ||
                !RadAction.isSuccess(sshFingerPrint)) {
            return new ProcessOutput(-1);
        }
        var aliasStr = alias.getStdout();
        var nidStr = nid.getStdout();
        var didStr = did.getStdout();
        var sshFingerPrintStr = sshFingerPrint.getStdout();
        var output = new ProcessOutput(0);
        output.appendStdout(aliasStr + nidStr + didStr + sshFingerPrintStr);
        return output;
    }

    public ProcessOutput fetchPeerChanges(RadPatch patch) {
        GitFetchSupport gfs = GitFetchSupport.fetchSupport(patch.repo.getProject());
        try {
            var gfr = gfs.fetchAllRemotes(List.of(patch.repo));
            gfr.showNotificationIfFailed();
        } catch (Exception e) {
            logger.warn("error fetching repo: {} for patch:{}", patch.repo, patch);
        }
        return new ProcessOutput(0);
        // var didParts = patch.author.id().split(":");
        // return executeCommand("git", patch.repo.getRoot().getPath(), List.of("fetch",
        //        didParts.length == 3 ? didParts[2] : ""), null);
    }

    public ProcessOutput clone(String urn, String directory) {
        return executeCommand(directory, List.of("clone", urn, "--no-confirm"), null);
    }

    public boolean isIdentityUnlocked(String key) {
        if (Strings.isNullOrEmpty(key)) {
            return false;
        }
        var output = executeCommand("ssh-add", "", ".", List.of("-l"), null, "");
        if (!RadAction.isSuccess(output)) {
            return false;
        }
        return output.getStdout().contains(key);
    }

    public ProcessOutput getVersion(String path) {
        if (Strings.isNullOrEmpty(path)) {
            return executeCommand(".", List.of("--version"), null);
        } else {
            return executeCommand(path, ".", List.of("--version"), null);
        }
    }

    public ProcessOutput trackRepo(RadTrack.Repo repo) {
        List<String> args = new ArrayList<>();
        args.add("track");
        args.add(repo.rid());
        if (!repo.scope().equals(RadTrack.Scope.NONE)) {
            args.add("--scope");
            args.add(repo.scope().value);
        }
        return executeCommand(".", args, null);
    }

    public ProcessOutput trackPeer(RadTrack.Peer peer) {
        List<String> args = new ArrayList<>();
        args.add("track");
        args.add(peer.nid());
        if (!Strings.isNullOrEmpty(peer.alias())) {
            args.add("--alias");
            args.add(peer.alias());
        }
        return executeCommand(".", args, null);
    }

    public ProcessOutput init(GitRepository root, String name, String description, String branch) {
        return executeCommand(root.getRoot().getPath(), List.of("init", "--name", name, "--description", description,
                "--default-branch", branch, "--no-confirm"), root);
    }

    public ProcessOutput auth(String passphrase, String alias, String radHome, String radPath) {
        if (!Strings.isNullOrEmpty(alias)) {
            return executeCommandWithStdin(".", radHome, radPath, List.of("auth", "--stdin", "--alias", alias), null, passphrase);
        }
        return executeCommandWithStdin(".", radHome, radPath, List.of("auth", "--stdin"), null, passphrase);
    }

    public ProcessOutput radWebJson(GitRepository repo) {
        final var projectSettings = projectSettingsHandler.loadSettings();
        final var httpdApiUrl = projectSettings.getSeedNode().url;
        return executeCommand(repo.getRoot().getPath(), List.of("web", "-b", httpdApiUrl, "--json"), repo);
    }

    public ProcessOutput remoteList(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("remote", "list"), root);
    }

    public ProcessOutput inspect(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("inspect"), root);
    }

    public ProcessOutput sync(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("sync", "-f"), root);
    }

    public ProcessOutput patchComment(GitRepository root, String patchId, String message) {
        if (SystemInfo.isWindows) {
            message = "'" + message + "'";
        }
        return executeCommand(root.getRoot().getPath(), List.of("comment", patchId, "--message", message), root);
    }

    public ProcessOutput executeCommandWithStdin(String workDir, String radHome, String radPath, List<String> args,
                                                 @Nullable GitRepository repo, String stdin) {
        final var projectSettings = projectSettingsHandler.loadSettings();
        final var path = Strings.isNullOrEmpty(radPath) ? projectSettings.getPath() : radPath;
        final var home = Strings.isNullOrEmpty(radHome) ? projectSettings.getRadHome() : radHome;
        return executeCommand(path, home, workDir, args, repo, stdin);
    }

    public ProcessOutput executeCommand(String workDir, List<String> args, @Nullable GitRepository repo) {
        final var projectSettings = projectSettingsHandler.loadSettings();
        final var radPath = projectSettings.getPath();
        final var radHome = projectSettings.getRadHome();
        return executeCommand(radPath, radHome, workDir, args, repo, "");
    }

    public ProcessOutput executeCommand(String exePath, String workDir, List<String> args, @Nullable GitRepository repo) {
        final var projectSettings = projectSettingsHandler.loadSettings();
        final var radHome = projectSettings.getRadHome();
        return executeCommand(exePath, radHome, workDir, args, repo, "");
    }

    public ProcessOutput executeCommand(
            String exePath, String radHome, String workDir, List<String> args, @Nullable GitRepository repo, String stdin) {
        ProcessOutput result;
        final var cmdLine = new GeneralCommandLine();
        var params = "";
        if (!Strings.isNullOrEmpty(radHome)) {
            params = "export RAD_HOME=" + radHome + "; " + exePath + " " + String.join(" ", args);
        } else {
            params = exePath + " " + String.join(" ", args);
        }
        if (SystemInfo.isWindows) {
            cmdLine.withExePath("wsl").withParameters("bash", "-ic").withParameters(params);
        } else {
            cmdLine.withExePath(exePath).withParameters(args);
            if (!Strings.isNullOrEmpty(radHome)) {
                cmdLine.withEnvironment("RAD_HOME", radHome);
            }
        }
        cmdLine.withCharset(StandardCharsets.UTF_8).withWorkDirectory(workDir)
                // we need parent environment to be present to our rad execution
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
                // make sure that the base directory containing our configured rad cli too. exists in the execution PATH
                .withEnvironment("PATH", new File(exePath).getParent() + File.pathSeparator +
                        cmdLine.getParentEnvironment().get("PATH"));
        try {
            var console = repo == null ? null : GitVcsConsoleWriter.getInstance(repo.getProject());
            if (console != null) {
                console.showCommandLine("[" + workDir + "] " + cmdLine.getCommandLineString());
            }
            if (!Strings.isNullOrEmpty(stdin)) {
                result = execAndGetOutputWithStdin(cmdLine, stdin);
            } else {
                result = execAndGetOutput(cmdLine);
            }
            if (console != null) {
                var stdout = result.getStdout();
                if (!Strings.isNullOrEmpty(stdout)) {
                    console.showMessage(stdout);
                }
                var stderr = result.getStderr();
                if (!Strings.isNullOrEmpty(stderr)) {
                    console.showErrorMessage(stderr);
                }
            }
            return result;
        } catch (ExecutionException ex) {
            logger.error("unable to execute rad command", ex);
            return new ProcessOutput(-1);
        }
    }

    public ProcessOutput execAndGetOutputWithStdin(GeneralCommandLine cmdLine, String stdin) {
        var output = ExecUtil.execAndGetOutput(cmdLine, stdin);
        var exitCode = Strings.isNullOrEmpty(output) ? -1 : 0;
        var pr = new ProcessOutput(exitCode);
        pr.appendStdout(output);
        return pr;
    }

    public ProcessOutput execAndGetOutput(GeneralCommandLine cmdLine) throws ExecutionException {
        return ExecUtil.execAndGetOutput(cmdLine, TIMEOUT);
    }
}
