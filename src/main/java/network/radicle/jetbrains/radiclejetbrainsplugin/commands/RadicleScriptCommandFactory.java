package network.radicle.jetbrains.radiclejetbrainsplugin.commands;

import com.intellij.openapi.util.SystemInfo;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

import java.util.List;

public class RadicleScriptCommandFactory {
    public static RadicleScriptCommand create(String workDir, String exePath, String radHome, List<String> args,
                                              RadicleProjectService service) {
        if (SystemInfo.isWindows) {
            return new WindowsScriptCommand(service, workDir, exePath, radHome, args);
        } else {
            return new LinuxScriptCommand(workDir, exePath, radHome, args);
        }
    }
}