package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.serviceContainer.NonInjectable;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class RadicleApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(RadicleApplicationService.class);

    private final RadicleSettingsHandler settingsHandler;

    public RadicleApplicationService() {
        this(new RadicleSettingsHandler());
    }

    @NonInjectable
    public RadicleApplicationService(RadicleSettingsHandler radicleSettingsHandler) {
        this.settingsHandler = radicleSettingsHandler;
    }

    public ProcessOutput getRadPath() {
        return executeCommand(".",".", List.of("which","rad"));
    }

    public ProcessOutput getVersion() {
        return executeCommand(".", List.of("--version"));
    }

    public ProcessOutput getVersion(String radPath) {
        return executeCommand(radPath, ".", List.of("--version"));
    }

    public ProcessOutput getSelf(GitRepository repo) {
        return executeCommand(repo.getRoot().getPath(), List.of("self"));
    }

    public ProcessOutput inspect(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("inspect"));
    }

    public ProcessOutput pull(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("pull"));
    }

    public ProcessOutput sync(GitRepository root) {
        return executeCommand(root.getRoot().getPath(), List.of("sync", "--branch",
                Objects.requireNonNull(root.getCurrentBranchName())));
    }

    public ProcessOutput executeCommand(String workDir, List<String> args) {
        final var settings = settingsHandler.loadSettings();
        final var radPath = settings.getPath();
        return executeCommand(radPath, workDir, args);
    }

    public static ProcessOutput executeCommand(String radPath, String workDir, List<String> args) {
        final var cmdLine = new GeneralCommandLine();
        if (SystemInfo.isWindows) {
            //TODO remove wsl
            cmdLine.withExePath("wsl").withParameters(radPath);
        } else {
            cmdLine.withExePath(radPath);
        }
        cmdLine.withCharset(StandardCharsets.UTF_8).withParameters(args).withWorkDirectory(workDir)
                // we need parent environment to be present to our rad execution
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
                // make sure that the base directory containing our configured rad cli too. exists in the execution PATH
                .withEnvironment("PATH", new File(radPath).getParent() + File.pathSeparator +
                        cmdLine.getParentEnvironment().get("PATH"));
        try {
            return ExecUtil.execAndGetOutput(cmdLine);
        } catch (ExecutionException ex) {
            logger.error("unable to execute rad command", ex);
            return new ProcessOutput(-1);
        }
    }
}
