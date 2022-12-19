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
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListUiUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import git4idea.changes.GitChangeUtils;
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

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static kotlinx.coroutines.CoroutineScopeKt.MainScope;

public class PatchListPanel {
    private final PatchTabController controller;
    private final Project project;
    private final ComboBox<SeedNode> seedNodeComboBox;
    private final DefaultListModel<RadPatch> seedModel;
    private final AsyncProcessIcon searchSpinner;
    private final RadicleSettingsHandler radicleSettingsHandler;
    private boolean triggerSeedNodeAction = true;
    protected BorderLayoutPanel mainPanel;

    public PatchListPanel(PatchTabController ctrl, Project project) {
        this.controller = ctrl;
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

        final var list = new JBList<>(seedModel);
        list.setCellRenderer(new PatchListCellRenderer());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != 1 || e.getClickCount() != 2) {
                    return;
                }
                final var selectedPatch = list.getSelectedValue();
                seedModel.clear();
                searchSpinner.setVisible(true);
                controller.createPatchProposalPanel(selectedPatch);
            }
        });
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
        mainPanel = JBUI.Panels.simplePanel();
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
        final var node = new RadTrack.SeedNode(url);
        for (GitRepository repo : radInitializedRepos) {
            var pull = new RadTrack(repo, node);
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

    /*
     * We need to parse output similar to the following into a list of available patches
     *
     * myradicleprj rad:git:hnrkm8s6xyxej7g198ycg5dd747wi5fddj8bo (pine.radicle.garden)
     * ├── hyymyxa6wa1kx9sfhfqxe7jkqmhwmgzcyaik6o6rj9y7dqfuzxq9uk       <-- peer ID
     * │   ├── main 7247e2b49d8cb17209cdae618740b37d05b0d5c0            <-- branch name and commit hash
     * │   └── test 02d9bb0cb16cc047c9566ad42213fdd48f103be3            <-- other branch and commit hash
     * │
     * ├── hybookffmi5m3rxbowq3rdoomufpxxddw48hb6b5zdpno7na44fhsc  you  <-- another peer ID (self)
     * │   └── main 7247e2b49d8cb17209cdae618740b37d05b0d5c0            <-- other branch and hash
     * │
     * └── hydk6tka3w8kkfamr1bzq6schmd43frjqiu3mw5eiwo31cci4s6qf6
     *     ├── main 7247e2b49d8cb17209cdae618740b37d05b0d5c0
     *     └── test 9411197feb2b47d0cf0f706e3aeff2a0b635fbcc
     */
    private List<RadPatch> parsePatchProposals(GitRepository repo, ProcessOutput output) {
        var infos = output.getStdoutLines(true);
        var radPatches = new ArrayList<RadPatch>();
        var currentUser = "";
        var self = false;
        for (var info : infos) {
            if (!info.contains("──")) {
                continue; // it's neither a line specifying a new user, nor a line specifying a branch, so skip it
            }
            if (info.startsWith("├") || info.startsWith("└")) {
                // a new user will be displayed in the "tree"
                var parts = info.split(" ");
                currentUser = parts[1];
                self = info.trim().endsWith("you");
            } else {
                // this is a line specifying a branch
                // skip first character which may be either space of | and then trim it (there are many spaces expected)
                var parts = info.substring(1).trim().split(" ");
                var branch = parts[1];
                var commit = parts[2];
                radPatches.add(new RadPatch(repo, currentUser, self, branch, commit));
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

    private static class PatchListCellRenderer implements ListCellRenderer<RadPatch> {
        private Map<Integer, Cell> cells;

        public PatchListCellRenderer() {
            cells = new HashMap<>();
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends RadPatch> list, RadPatch value, int index, boolean isSelected, boolean cellHasFocus) {
            if (!cells.containsKey(index)) {
                var cell = new Cell(index, value);
                cells.put(index, cell);
            }
            var cell = cells.get(index);
            cell.setBackground(ListUiUtil.WithTallRow.INSTANCE.background(list, isSelected, list.hasFocus()));
            cell.text.setForeground(ListUiUtil.WithTallRow.INSTANCE.foreground(isSelected, list.hasFocus()));
            return cell;
        }

        public static class Cell extends JPanel {
            public final int index;
            public final JLabel text;
            public final RadPatch patch;

            public Cell(int index, RadPatch patch) {
                this.index = index;
                this.patch = patch;

                var gapAfter = JBUI.scale(5);
                var patchPanel = new JPanel();
                patchPanel.setOpaque(false);
                patchPanel.setBorder(JBUI.Borders.empty(10, 8));
                patchPanel.setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0")
                        .insets("0", "0", "0", "0")
                        .fillX()));

                setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0").noGrid()
                        .insets("0", "0", "0", "0")
                        .fillX()));

                var innerPanel = new JPanel();
                innerPanel.setLayout(new BorderLayout());
                text = new JLabel(patch.repo.getRoot().getName() + " - " + patch.commitHash);
                patchPanel.add(text, BorderLayout.NORTH);
                var comment = new JLabel("Created by: " + (patch.self ? "You " : "") + patch.peerId);
                comment.setForeground(Color.GRAY);
                patchPanel.add(comment, BorderLayout.SOUTH);
                add(patchPanel, new CC().minWidth("0").gapAfter("push"));
            }

            @Override
            public AccessibleContext getAccessibleContext() {
                var ac = super.getAccessibleContext();
                ac.setAccessibleName(patch.repo.getRoot().getName() + " - " + patch.peerId);
                return ac;
            }
        }
    }
}
