package network.radicle.jetbrains.radiclejetbrainsplugin.listeners;

import com.intellij.dvcs.push.PrePushHandler;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadiclePrePushListener implements PrePushHandler {

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getPresentableName() {
        //TODO what i have to return here
        return "";
    }

    @Override
    public @NotNull Result handle(@NotNull List<PushInfo> pushDetails, @NotNull ProgressIndicator indicator) {
        Map<Project , List<PushInfo>> map = new HashMap<>();
        for (var pi : pushDetails) {
            var p = pi.getRepository().getProject();
            map.computeIfAbsent(p, prj -> new ArrayList<>()).add(pi);
        }
        for (var e : map.entrySet()) {
            var rps = e.getKey().getService(RadicleProjectService.class);
            rps.pushDetails = e.getValue();
        }
        return Result.OK;
    }

}
