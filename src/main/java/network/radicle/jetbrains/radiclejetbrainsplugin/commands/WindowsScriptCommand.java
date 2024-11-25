package network.radicle.jetbrains.radiclejetbrainsplugin.commands;

import com.google.common.base.Strings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

import java.io.File;
import java.util.List;

public class WindowsScriptCommand extends RadicleScriptCommand {
    private final WSLDistribution wslDistribution;

    public WindowsScriptCommand(RadicleProjectService service, String workDir, String exePath, String radHome,
                                List<String> args, Project project) {
        super(workDir, exePath, radHome, args, project);
        this.wslDistribution = new WSLDistribution(service.getWslDistro());
    }

    @Override
    public File getTempPath() {
        var path = wslDistribution.getWindowsPath(TEMP_FOLDER_PATH);
        if (Strings.isNullOrEmpty(path)) {
            return null;
        }
        return new File(path);
    }

    @Override
    public GeneralCommandLine getCommandLine() {
        var path = createTempExecutableScript();
        if (path == null) {
            return null;
        }
        var wslPath = wslDistribution.getWslPath(path.toString());
        if (Strings.isNullOrEmpty(wslPath)) {
            return null;
        }
        var cmdLine = new GeneralCommandLine();
        cmdLine.setWorkDirectory(workDir);
        cmdLine.withExePath("wsl").withParameters("bash", "-ic").withParameters("bash " + wslPath);
        return cmdLine;
    }
}
