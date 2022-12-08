package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.dvcs.repo.VcsRepositoryMappingListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListUiUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone.CloneRadDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PatchListPanel {
    private Project project;
    private ComboBox<SeedNode> seedNodeComboBox;
    protected DefaultListModel<RadPatch> seedModel;
    private AsyncProcessIcon searchSpinner;

    public PatchListPanel(Project project) {
        this.project = project;
        this.searchSpinner = new AsyncProcessIcon(RadicleBundle.message("loadingProjects"));
        this.seedModel = new DefaultListModel<>();
        registerVcsChangeListener(project);
    }

    private void initializeSeedNodeCombobox() {
        RadicleSettings settings = new RadicleSettingsHandler().loadSettings();
        seedNodeComboBox = new ComboBox<>();
        seedNodeComboBox.setRenderer(new CloneRadDialog.SeedNodeCellRenderer());
        seedNodeComboBox.setSelectedIndex(-1);
        for (var node : settings.getSeedNodes()) {
            seedNodeComboBox.addItem(node);
        }
    }

    public JComponent create() {
        initializeSeedNodeCombobox();
        var mainPanel = JBUI.Panels.simplePanel();
        seedNodeComboBox.addActionListener(new SeedNodeChangeListener());
        var borderPanel = new JPanel(new BorderLayout(5, 5));
        borderPanel.add(new JBLabel(RadicleBundle.message("selectSeedNode")), BorderLayout.NORTH);
        Presentation presentation = new Presentation();
        presentation.setIcon(AllIcons.Actions.BuildAutoReloadChanges);
        borderPanel.add(new ActionButton(new RefreshSeedNodeAction(),
                presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE), BorderLayout.EAST);
        borderPanel.add(seedNodeComboBox, BorderLayout.CENTER);
        var list = new JBList<>(seedModel);
        list.setCellRenderer(new PatchListCellRenderer());
        list.setExpandableItemsEnabled(false);
        list.getEmptyText().clear();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ScrollingUtil.installActions(list);
        ListUtil.installAutoSelectOnMouseMove(list);
        ListUiUtil.Selection.INSTANCE.installSelectionOnFocus(list);
        ListUiUtil.Selection.INSTANCE.installSelectionOnRightClick(list);


        var pane = new JPanel(new BorderLayout());
        var scrollPane = ScrollPaneFactory.createScrollPane(pane, true);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setOpaque(true);
        pane.add(list, BorderLayout.NORTH);
        pane.add(searchSpinner, BorderLayout.CENTER);

        searchSpinner.setVisible(false);
        mainPanel.addToCenter(scrollPane);
        mainPanel.addToTop(borderPanel);
        registerVcsChangeListener(project);
        return mainPanel;
    }

    private List<RadPatch> getPatchProposals(List<GitRepository> repos, String url) {
        var outputs = new ArrayList<RadPatch>();
        var radInitializedRepos = BasicAction.getInitializedReposWithNodeConfigured(repos, true);
        if (radInitializedRepos.isEmpty()) {
            return List.of();
        }
        var updateCountDown = new CountDownLatch(radInitializedRepos.size());
        for (GitRepository repo : radInitializedRepos) {
            var pull = new RadTrack(repo, url);
            ProcessOutput output = new BasicAction(pull, repo.getProject(), updateCountDown).perform();
            if (output.getExitCode() == 0) {
                var radPatch = parsePatchProposals(repo, output);
                if (radPatch != null) {
                    outputs.addAll(radPatch);
                }
            }
        }
        return outputs;
    }

    private List<RadPatch> parsePatchProposals(GitRepository repo, ProcessOutput output) {
        var infos = output.getStdoutLines(true);
        var radPatches = new ArrayList<RadPatch>();
        for (String info : infos) {
            var parts = info.split(" ");
            if (parts.length == 2 || info.contains("you")) {
                radPatches.add(new RadPatch(parts[1], repo));
            }
        }
        return radPatches;
    }

    private void registerVcsChangeListener(final Project project) {
        VcsRepositoryMappingListener vcsListener = this::updateListPanel;
        project.getMessageBus().connect().subscribe(VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED, vcsListener);
    }

    private void updateListPanel() {
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        var selectedSeedNode = (SeedNode) seedNodeComboBox.getSelectedItem();
        var url = "http://" + selectedSeedNode.host;

        seedModel.clear();
        searchSpinner.setVisible(true);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var patchProposals = getPatchProposals(repos, url);
            ApplicationManager.getApplication().invokeLater(() -> {
                seedModel.addAll(patchProposals);
                searchSpinner.setVisible(false);
            });
        });
    }

    private class SeedNodeChangeListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (seedNodeComboBox.getSelectedItem() != null) {
                updateListPanel();
            }
        }
    }

    private class RefreshSeedNodeAction extends AnAction {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // initializeSeedNodeCombobox();
            updateListPanel();
        }
    }

    private static class PatchListCellRenderer extends JPanel implements ListCellRenderer<RadPatch> {
        private final JLabel title = new JLabel();
        private final JPanel patchPanel;
        public PatchListCellRenderer() {
            patchPanel = new JPanel();
            var gapAfter = JBUI.scale(5);
            patchPanel.setOpaque(false);
            patchPanel.setBorder(JBUI.Borders.empty(10, 8));
            patchPanel.setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0")
                    .insets("0", "0", "0", "0")
                    .fillX()));

            this.setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0").noGrid()
                    .insets("0", "0", "0", "0")
                    .fillX()));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends RadPatch> list,
                                                      RadPatch value, int index, boolean isSelected, boolean cellHasFocus) {
            setBackground(ListUiUtil.WithTallRow.INSTANCE.background(list, isSelected, list.hasFocus()));
            var primaryTextColor = ListUiUtil.WithTallRow.INSTANCE.foreground(isSelected, list.hasFocus());
            title.setText(value.peerId);
            title.setForeground(primaryTextColor);
            patchPanel.add(title);
            add(patchPanel, new CC().minWidth("0").gapAfter("push"));
            return this;
        }
    }

}
