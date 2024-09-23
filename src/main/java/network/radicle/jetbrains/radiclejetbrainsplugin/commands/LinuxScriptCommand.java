package network.radicle.jetbrains.radiclejetbrainsplugin.commands;
import com.intellij.execution.configurations.GeneralCommandLine;
import java.util.List;

public class LinuxScriptCommand extends RadicleScriptCommand {

    public LinuxScriptCommand(String workDir, String exePath, String radHome, List<String> args) {
        super(workDir, exePath, radHome, args);
    }

    @Override
    public GeneralCommandLine getCommandLine() {
        var path = createTempExecutableScript();
        if (path == null) {
            return null;
        }
        var cmdLine = new GeneralCommandLine();
        cmdLine.setWorkDirectory(workDir);
        cmdLine.withExePath("bash").withParameters(path.toString());
        return cmdLine;
    }
}
