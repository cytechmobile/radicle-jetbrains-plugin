package network.radicle.jetbrains.radiclejetbrainsplugin.patches;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;

import java.awt.*;

public class PatchTabController {
    private Project project;
    private Content tab;

    public PatchTabController(Content tab, Project project) {
        this.tab = tab;
        this.project = project;
        viewPatches();
    }

    public Unit viewPatches()  {
        tab.setDisplayName(RadicleBundle.message("patchTabName"));
        var mainPanel = tab.getComponent();
        var patchPanel = new PatchListPanel(project).create();

        mainPanel.setLayout(new BorderLayout(5,10));
        mainPanel.removeAll();
        mainPanel.add(patchPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();

        return Unit.INSTANCE;
    }



}
