package network.radicle.jetbrains.radiclejetbrainsplugin.patches;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;

import java.awt.*;

public class PatchTabController {
    private Project project;
    private Content tab;
    private PatchListPanel panel;

    public PatchTabController(Content tab, Project project) {
        this.tab = tab;
        this.project = project;
        this.panel = new PatchListPanel(project);
    }

    public void createPatchesPanel()  {
        tab.setDisplayName(RadicleBundle.message("patchTabName"));
        var mainPanel = tab.getComponent();
        var createdPanel = panel.create();

        mainPanel.setLayout(new BorderLayout(5, 10));
        mainPanel.removeAll();
        mainPanel.add(createdPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }
}
