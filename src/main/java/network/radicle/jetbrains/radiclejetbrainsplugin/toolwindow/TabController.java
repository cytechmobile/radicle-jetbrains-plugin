package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import java.awt.BorderLayout;
public abstract class TabController {
    protected final Project project;
    protected final Content tab;
    protected final ProjectApi myApi;

    public TabController(Project project, Content tab, ProjectApi myApi) {
        this.project = project;
        this.tab = tab;
        this.myApi = myApi;
    }

    public void createPanel() {
        tab.setDisplayName(getTabName());
        var mainPanel = tab.getComponent();
        mainPanel.setLayout(new BorderLayout(5, 10));
        mainPanel.removeAll();
        var panel = getPanel();
        mainPanel.add(panel.create(), BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public abstract String getTabName();
    public abstract ListPanel getPanel();
    public Disposable getDisposer() {
        return tab.getDisposer();
    }
}
