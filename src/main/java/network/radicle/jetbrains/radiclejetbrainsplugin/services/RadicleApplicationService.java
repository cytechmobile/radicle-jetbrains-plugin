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
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class RadicleApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(RadicleApplicationService.class);

    private final RadicleSettingsHandler settingsHandler;
    private static final String RAD_PASSPHRASE="RAD_PASSPHRASE";
    private String radConfigPath = "";

    public RadicleApplicationService() {
        this(new RadicleSettingsHandler());
    }

    @NonInjectable
    public RadicleApplicationService(RadicleSettingsHandler radicleSettingsHandler) {
        this.settingsHandler = radicleSettingsHandler;
    }

    public void setRadConfigPath(String path) {
        this.radConfigPath = path;
    }

    public String getRadConfigPath() {
        return this.radConfigPath;
    }

    public ProcessOutput getRadPath() {
        return executeCommand(".", ".", List.of("which","rad"), null);
    }

    public ProcessOutput self(boolean activeProfile) {
        if (activeProfile) {
            return executeCommand(".",List.of("self","--profile"),null);
        } else {
            return executeCommand(".",List.of("self"),null);
        }
    }

    public ProcessOutput clone(String urn,String directory) {
        return executeCommand(directory,List.of("clone",urn),null);
    }

    public ProcessOutput getVersion(String path) {
        if (Strings.isNullOrEmpty(path)) {
            return executeCommand(".", List.of("--version"), null);
        } else {
            return executeCommand(path,".", List.of("--version"), null);
        }
    }

    public ProcessOutput removeIdentity(String profile) {
        return executeCommand(".",List.of("rm","--no-confirm","--no-passphrase",profile),null);
    }

    private ProcessOutput setEnvVar(String var,String value) {
        var removeVarPr = executeCommand("",".",List.of("sed","-i","/RAD_PASSPHRASE/d","~/.bashrc"),null);
        var addVarPr = executeCommand("",".",List.of("echo","export",var + "=" + value,">>","~/.bashrc"),null);
        if (removeVarPr.getExitCode() == 0 && addVarPr.getExitCode() == 0) {
            return new ProcessOutput(0);
        }
        return new ProcessOutput(-1);
    }

    public ProcessOutput auth(String name,String passphrase,boolean setDefaultProfile) {
        var output = setEnvVar(RAD_PASSPHRASE,passphrase);
        if (output.getExitCode() != 0) {
            var newOutput = new ProcessOutput(-1);
            newOutput.appendStderr("Unable to set RAD_PASSPHRASE in bashrc file");
            return newOutput;
        }
        if (setDefaultProfile) {
            return executeCommand(".",List.of("auth","--stdin",name),null);
        } else {
            return executeCommand(".",List.of("auth","--init","--name",name),null);
        }
    }

    public ProcessOutput push(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("push"), root);
    }

    public ProcessOutput pull(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("pull"), root);
    }

    public ProcessOutput sync(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("sync", "--branch",
                Objects.requireNonNull(root.getCurrentBranchName())), root);
    }

    public ProcessOutput executeCommand(String workDir, List<String> args, @Nullable GitRepository repo) {
        final var settings = settingsHandler.loadSettings();
        final var radPath = settings.getPath();
        return executeCommand(radPath, workDir, args, repo);
    }

    public ProcessOutput executeCommand(
            String radPath, String workDir, List<String> args, @Nullable GitRepository repo) {
        final var cmdLine = new GeneralCommandLine();
        if (SystemInfo.isWindows) {
            //TODO remove wsl
            cmdLine.withExePath("wsl").withParameters("bash","-ic").withParameters(radPath + " " + String.join(" ",args));
        } else {
            cmdLine.withExePath(radPath).withParameters(args);
        }
        cmdLine.withCharset(StandardCharsets.UTF_8).withWorkDirectory(workDir)
                // we need parent environment to be present to our rad execution
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
                // make sure that the base directory containing our configured rad cli too. exists in the execution PATH
                .withEnvironment("PATH", new File(radPath).getParent() + File.pathSeparator +
                        cmdLine.getParentEnvironment().get("PATH"));
        try {
            var console = repo == null ? null : GitVcsConsoleWriter.getInstance(repo.getProject());
            if (console != null) {
                console.showCommandLine("[" + workDir + "] " + cmdLine.getCommandLineString());
            }
            var result = execAndGetOutput(cmdLine);

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

    public ProcessOutput execAndGetOutput(GeneralCommandLine cmdLine) throws ExecutionException {
        return ExecUtil.execAndGetOutput(cmdLine);
    }
}
