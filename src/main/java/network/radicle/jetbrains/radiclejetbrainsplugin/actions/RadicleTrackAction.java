package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.TrackDialog;
import org.jetbrains.annotations.NotNull;

public class RadicleTrackAction extends AnAction  {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!RadAction.isCliPathConfigured(e.getProject())) {
            return;
        }
        var project = e.getProject();
        var trackDialog = new TrackDialog(project);
        trackDialog.showAndGet();
    }
}
