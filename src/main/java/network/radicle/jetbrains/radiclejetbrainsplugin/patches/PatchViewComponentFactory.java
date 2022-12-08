package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

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
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListUiUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone.CloneRadDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class PatchViewComponentFactory {
    private Project project;
    private ComboBox<SeedNode> seedNodeComboBox;
    private final RadicleSettings settings;

    private final HashMap<GitRepository, ArrayList<Object>> myPeers;

    protected DefaultListModel<String> seedModel;


    public PatchViewComponentFactory(Project project) {
        this.project = project;
        this.settings = new RadicleSettingsHandler().loadSettings();
        this.myPeers = new HashMap<>();
        this.seedModel = new DefaultListModel<>();
    }
    private void initializeSeedNodeCombobox() {
        seedNodeComboBox = new ComboBox<>();
        seedNodeComboBox.setRenderer(new CloneRadDialog.SeedNodeCellRenderer());
        seedNodeComboBox.setSelectedIndex(-1);
        for (var node : settings.getSeedNodes()) {
            seedNodeComboBox.addItem(node);
        }
    }

    public JComponent createPatchComponent() {
        initializeSeedNodeCombobox();
        var mainPanel = JBUI.Panels.simplePanel();
        seedNodeComboBox.addActionListener(new SeedNodeListener());
        seedNodeComboBox.setSelectedIndex(0);
        var borderPanel = new JPanel(new BorderLayout(5,5));
        borderPanel.add(new JBLabel(RadicleBundle.message("selectSeedNode")), BorderLayout.NORTH);

        Presentation presentation = new Presentation();
        presentation.setIcon(AllIcons.Actions.BuildAutoReloadChanges);
        borderPanel.add(new ActionButton( new RefreshSeedNodeAction(),
                presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE), BorderLayout.EAST);
        borderPanel.add(seedNodeComboBox,BorderLayout.CENTER);
        var list = new JBList<>(seedModel);
        list.setExpandableItemsEnabled(false);
        list.getEmptyText().clear();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ScrollingUtil.installActions(list);
        ListUtil.installAutoSelectOnMouseMove(list);
        ListUiUtil.Selection.INSTANCE.installSelectionOnFocus(list);
        ListUiUtil.Selection.INSTANCE.installSelectionOnRightClick(list);

        var scrollPane = ScrollPaneFactory.createScrollPane(list,true);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setOpaque(true);


        mainPanel.addToCenter(scrollPane);
        mainPanel.addToTop(borderPanel);
        return mainPanel;
    }

    private class SeedNodeListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (seedNodeComboBox.getSelectedItem() != null) {
                getAndShowPeers();
            }
        }
    }

    private void getAndShowPeers() {
        var selectedSeedNode = (SeedNode) seedNodeComboBox.getSelectedItem();
        var url = "http://" + selectedSeedNode.host;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var gitRepoManager = GitRepositoryManager.getInstance(project);
            var repos = gitRepoManager.getRepositories();
            var radInitializedRepos = BasicAction.getInitializedReposWithNodeConfigured(repos, true);
            if (radInitializedRepos.isEmpty()) {
                return ;
            }
            var updateCountDown = new CountDownLatch(radInitializedRepos.size());
            radInitializedRepos.forEach(repo -> {
                var pull = new RadTrack(repo,url);
                ProcessOutput output = new BasicAction(pull, repo.getProject(), updateCountDown).perform();
                if (output.getExitCode() == 0) {
                    var peers = output.getStdoutLines(true);
                    var peersId = new ArrayList<>();
                    for(String peer : peers) {
                        var spl = peer.split(" ");
                        if (spl.length == 2 || peer.contains("you")) {
                            peersId.add(spl[1]);
                        }
                    }
                    myPeers.put(repo,peersId);
                }
            });
            ApplicationManager.getApplication().invokeLater(() -> {
                seedModel.clear();
                myPeers.forEach((key, value) -> {
                    value.forEach(el -> seedModel.addElement((String) el));
                });
                //TODO update ui
            });
        });
    }

    private class RefreshSeedNodeAction extends AnAction {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getAndShowPeers();
           //TODO rad track --seed <url> --remote | load seed nodes
        }
    }

}
