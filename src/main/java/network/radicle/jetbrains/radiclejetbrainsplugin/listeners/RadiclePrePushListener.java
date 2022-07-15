package network.radicle.jetbrains.radiclejetbrainsplugin.listeners;

import com.intellij.dvcs.push.PrePushHandler;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RadiclePrePushListener implements PrePushHandler {
    private static List<PushInfo> pushDetails = List.of();

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getPresentableName() {
        //TODO what i have to return here
        return "";
    }

    @Override
    public @NotNull Result handle(@NotNull List<PushInfo> pushDetails, @NotNull ProgressIndicator indicator) {
        RadiclePrePushListener.pushDetails = pushDetails;
        return Result.OK;
    }

    public static List<PushInfo> getPushDetails() {
        return RadiclePrePushListener.pushDetails;
    }
}
