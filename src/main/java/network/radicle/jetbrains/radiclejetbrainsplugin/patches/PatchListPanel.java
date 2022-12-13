package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
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
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.repo.GitRepositoryManager;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
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

import static kotlinx.coroutines.CoroutineScopeKt.MainScope;

public class PatchListPanel {
    private final Project project;
    private final ComboBox<SeedNode> seedNodeComboBox;
    private final DefaultListModel<RadPatch> seedModel;
    private final AsyncProcessIcon searchSpinner;
    private final RadicleSettingsHandler radicleSettingsHandler;
    private boolean triggerSeedNodeAction = true;

    public PatchListPanel(Project project) {
        this.project = project;
        this.searchSpinner = new AsyncProcessIcon(RadicleBundle.message("loadingProjects"));
        this.seedModel = new DefaultListModel<>();
        this.radicleSettingsHandler = new RadicleSettingsHandler();
        this.seedNodeComboBox = new ComboBox<>();
        seedNodeComboBox.setRenderer(new CloneRadDialog.SeedNodeCellRenderer());
        registerVcsChangeListener(project);
    }

    private void initializeSeedNodeCombobox() {
        var settings = radicleSettingsHandler.loadSettings();
        var loadedSeedNodes = settings.getSeedNodes();
        seedNodeComboBox.removeAllItems();
        for (var node : loadedSeedNodes) {
            seedNodeComboBox.addItem(node);
        }
    }

    public JComponent create() {
        initializeSeedNodeCombobox();

        seedNodeComboBox.addActionListener(new SeedNodeChangeListener());
        var borderPanel = new JPanel(new BorderLayout(5, 5));
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
        var mainPanel = JBUI.Panels.simplePanel();
        mainPanel.addToCenter(scrollPane);
        var scope = MainScope();
        var history = new PatchSearchHistoryModel();
        var searchVm = new PatchSearchPanelViewModel(scope,history,project);
        var searchPanel = new PatchSearchPanel(searchVm).create(scope);
        borderPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.addToTop(borderPanel);
        return mainPanel;
    }

    private List<RadPatch> getPatchProposals(List<GitRepository> repos, String url) {
        var outputs = new ArrayList<RadPatch>();
        var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, true);
        if (radInitializedRepos.isEmpty()) {
            return List.of();
        }
        final var updateCountDown = new CountDownLatch(radInitializedRepos.size());
        for (GitRepository repo : radInitializedRepos) {
            var pull = new RadTrack(repo, url);
            ProcessOutput output = pull.perform(updateCountDown);
            if (output.getExitCode() == 0) {
                var radPatch = parsePatchProposals(repo, output);
                if (!radPatch.isEmpty()) {
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
        project.getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE,
                (GitRepositoryChangeListener) repository -> updateListPanel());
    }

    private void updateListPanel() {
        var selectedSeedNode = (SeedNode) seedNodeComboBox.getSelectedItem();
        if (selectedSeedNode == null) {
            return ;
        }
        var url = "http://" + selectedSeedNode.host;
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            seedModel.clear();
            searchSpinner.setVisible(true);
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
            if (seedNodeComboBox.getSelectedItem() != null && triggerSeedNodeAction) {
                updateListPanel();
            }
        }
    }

    private class RefreshSeedNodeAction extends AnAction {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            triggerSeedNodeAction = false;
            var prevSelectedIndex = seedNodeComboBox.getSelectedIndex();
            initializeSeedNodeCombobox();
            seedNodeComboBox.setSelectedIndex(prevSelectedIndex);
            triggerSeedNodeAction = true;
            updateListPanel();
        }
    }

    private static class PatchListCellRenderer extends JPanel implements ListCellRenderer<RadPatch> {
        private final JLabel title = new JLabel();
        private final JPanel patchPanel;
        public PatchListCellRenderer() {
            var gapAfter = JBUI.scale(5);
            patchPanel = new JPanel();
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
