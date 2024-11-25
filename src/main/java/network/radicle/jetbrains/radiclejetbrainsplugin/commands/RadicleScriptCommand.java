package network.radicle.jetbrains.radiclejetbrainsplugin.commands;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public abstract class RadicleScriptCommand {
    public static final Logger logger = LoggerFactory.getLogger(RadicleScriptCommand.class);
    public static final String PREFIX = "radicle_script";
    public static final String SUFFIX = ".sh";
    public static final String TEMP_FOLDER_PATH = "/tmp";

    public File tempFile;
    public String workDir;
    public String radHome;
    public String exePath;
    public String shebang;
    public List<String> args;
    public RadicleCliService cliService;

    public RadicleScriptCommand(String workDir, String shebang, String exePath, String radHome, List<String> args, Project project) {
        this.workDir = workDir;
        this.shebang = shebang;
        this.exePath = exePath;
        this.radHome = radHome;
        this.args = args;
        this.cliService = project.getService(RadicleCliService.class);
    }

    public RadicleScriptCommand(String workDir, String exePath, String radHome, List<String> args, Project project) {
        this(workDir, "#!/bin/bash", exePath, radHome, args, project);
    }

    public File createTempExecutableScript() {
        var tmpPath = getTempPath();
        try {
            var scriptName = getRandomScriptName();
            tempFile = FileUtil.createTempFile(tmpPath, scriptName, SUFFIX, true);
            var command = getCommand();
            FileUtil.writeToFile(tempFile, command.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            logger.warn("Unable to create / write to a file");
        }
        return tempFile;
    }

    public String getCommand() {
        var command = String.join(" ", args);
        return shebang + "\n" + getExportRadPassphrase() +
                getExportRadHomeCommand() + exePath + " " + command;
    }

    public File getTempPath() {
        return new File(TEMP_FOLDER_PATH);
    }

    public void deleteTempFile() {
        try {
            Files.delete(tempFile.toPath());
        } catch (Exception e) {
            logger.warn("Unable to delete the temp file");
        }
    }

    public abstract GeneralCommandLine getCommandLine();

    private String getExportRadHomeCommand() {
        return "export RAD_HOME=" + radHome + "\n";
    }

    private String getExportRadPassphrase() {
        var currentIdentity = cliService.getCurrentIdentity();
        if (currentIdentity == null) {
            return "";
        }
        var projectSettings = new RadicleProjectSettingsHandler(cliService.getProject());
        var password = projectSettings.getPassword(currentIdentity.nodeId);
        if (Strings.isNullOrEmpty(password)) {
            return "";
        }
        return "export RAD_PASSPHRASE=" + password + "\n";
    }

    private String getRandomScriptName() {
        var randomStr = UUID.randomUUID().toString();
        var firstPart = randomStr.split("-")[0];
        return PREFIX + "_" + firstPart;
    }
}
