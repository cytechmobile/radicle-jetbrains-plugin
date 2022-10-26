package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.google.common.base.Strings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.serviceContainer.NonInjectable;
import git4idea.repo.GitRepository;
import git4idea.util.GitVcsConsoleWriter;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadConfig;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RadicleApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(RadicleApplicationService.class);
    private static final int TIMEOUT = 10000;
    private final RadicleSettingsHandler settingsHandler;
    private Map<String, String> radStoragePath = null;

    public RadicleApplicationService() {
        this(new RadicleSettingsHandler());
    }

    @NonInjectable
    public RadicleApplicationService(RadicleSettingsHandler radicleSettingsHandler) {
        this.settingsHandler = radicleSettingsHandler;
    }

    public void setRadConfigPaths(String gitStoragePath, String keysStoragePath) {
        if (radStoragePath == null) {
            radStoragePath = new HashMap<>();
        }
        radStoragePath.put("gitStoragePath", gitStoragePath);
        radStoragePath.put("keysStoragePath", keysStoragePath);
    }

    public RadConfig getRadStoragePath() {
        if (radStoragePath == null) {
            return null;
        }
        return new RadConfig(radStoragePath.get("gitStoragePath"), radStoragePath.get("keysStoragePath"));
    }

    public ProcessOutput getRadPath() {
        return executeCommand("which", ".", List.of("rad"), null, false);
    }

    public ProcessOutput self (boolean activeProfile) {
        if (activeProfile) {
            return executeCommand(".",List.of("self","--profile"),null);
        } else {
            return executeCommand(".",List.of("self"),null);
        }
    }

    public ProcessOutput clone(String urn,String directory) {
        return executeCommand(directory,List.of("clone",urn,"--no-confirm"),null);
    }

    public ProcessOutput getVersion(String path) {
        if (Strings.isNullOrEmpty(path)) {
            return executeCommand(".", List.of("--version"), null);
        } else {
            return executeCommand(path,".", List.of("--version"), null, false);
        }
    }

    public ProcessOutput init(GitRepository root,String name, String description, String branch) {
        return executeCommand(root.getRoot().getPath(), List.of("init","--name",name,"--description",description,
                "--default-branch",branch,"--no-confirm"),root);
    }

    public ProcessOutput auth(String name,String passphrase,boolean setDefaultProfile) {
        if (setDefaultProfile) {
            return executeCommandWithStdin(".",List.of("auth",name),null);
        } else {
            return executeCommand(".",List.of("auth","--init","--name",name,"--passphrase",passphrase),null);
        }
    }

    public ProcessOutput inspect(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("inspect"),root);
    }

    public ProcessOutput push(GitRepository root, String seed) {
        return executeCommand(root.getRoot().getPath(), List.of("push","--seed", seed), root);
    }

    public ProcessOutput pull(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("pull"), root);
    }

    public ProcessOutput sync(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("sync", "--branch",
                Objects.requireNonNull(root.getCurrentBranchName())), root);
    }

    public ProcessOutput executeCommandWithStdin(String workDir, List<String> args, @Nullable GitRepository repo) {
        final var settings = settingsHandler.loadSettings();
        final var radPath = settings.getPath();
        return executeCommand(radPath, workDir, args, repo, true);
    }

    public ProcessOutput executeCommand(String workDir, List<String> args, @Nullable GitRepository repo) {
        final var settings = settingsHandler.loadSettings();
        final var radPath = settings.getPath();
        return executeCommand(radPath, workDir, args, repo, false);
    }

    public ProcessOutput executeCommand(
            String exePath, String workDir, List<String> args, @Nullable GitRepository repo, boolean isDefaultIdentityAction) {
        ProcessOutput result ;
        final var cmdLine = new GeneralCommandLine();
        if (SystemInfo.isWindows) {
            //TODO remove wsl
            cmdLine.withExePath("wsl").withParameters("bash","-ic").withParameters(exePath + " " + String.join(" ",args));
        } else {
            cmdLine.withExePath(exePath).withParameters(args);
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

            if (isDefaultIdentityAction) {
                 result = execAndGetOutputWithStdin(cmdLine);
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

    public ProcessOutput execAndGetOutputWithStdin(GeneralCommandLine cmdLine) {
        var output = ExecUtil.execAndGetOutput(cmdLine,"");
        var exitCode = Strings.isNullOrEmpty(output) ? -1 : 0;
        var msg = Strings.isNullOrEmpty(output) ? RadicleBundle.message("setDefaultIdentityError") : "";
        var pr = new ProcessOutput(exitCode);
        pr.appendStderr(msg);
        return pr;
    }

    public ProcessOutput execAndGetOutput(GeneralCommandLine cmdLine) throws ExecutionException {
        return ExecUtil.execAndGetOutput(cmdLine,TIMEOUT);
    }
}
