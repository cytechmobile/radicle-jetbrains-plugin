package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;

import java.awt.BorderLayout;

public abstract class TabController<S, Q extends ReviewListSearchValue, P extends SearchViewModelBase<Q, ?, S>> {
    protected final Project project;
    protected final Content tab;

    public TabController(Project project, Content tab) {
        this.project = project;
        this.tab = tab;
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
    public abstract ListPanel<S, Q, P> getPanel();
    public Disposable getDisposer() {
        return tab.getDisposer();
    }
}
