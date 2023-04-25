package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.google.common.base.Strings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListUiUtil;
import com.intellij.vcs.log.ui.frame.ProgressStripe;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CoroutineScopeKt;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInspect;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.ListCellRenderer;
import javax.swing.JList;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static kotlinx.coroutines.CoroutineScopeKt.MainScope;

public class PatchListPanel {
    private final PatchTabController controller;
    private final Project project;
    private final DefaultListModel<RadPatch> patchModel;
    private final RadicleProjectSettingsHandler radicleProjectSettingsHandler;
    private List<RadPatch> loadedRadPatches;
    private ProgressStripe progressStripe;
    private PatchSearchPanelViewModel searchVm;
    private JBList<RadPatch> patchesList;
    private final ProjectApi myApi;

    public PatchListPanel(PatchTabController ctrl, Project project, ProjectApi api) {
        this.myApi = api;
        this.controller = ctrl;
        this.project = project;
        this.patchModel = new DefaultListModel<>();
        this.radicleProjectSettingsHandler = new RadicleProjectSettingsHandler(project);
    }

    public JComponent create() {
        var scope = MainScope();
        Disposer.register(controller.getDisposer(), () -> CoroutineScopeKt.cancel(scope, null));
        searchVm = new PatchSearchPanelViewModel(scope, new PatchSearchHistoryModel(), project);
        var filterPanel = new PatchFilterPanel(searchVm).create(scope);
        var verticalPanel = new JPanel(new VerticalLayout(5));
        verticalPanel.add(filterPanel);
        var mainPanel = JBUI.Panels.simplePanel();
        var listPanel = createListPanel();
        mainPanel.addToTop(verticalPanel);
        mainPanel.addToCenter(listPanel);

        searchVm.getSearchState().collect((patchListSearchValue, continuation) -> {
            filterList(patchListSearchValue);
            return null;
        }, new Continuation<Object>() {
            @Override
            public void resumeWith(@NotNull Object o) {

            }

            @NotNull
            @Override
            public CoroutineContext getContext() {
                return scope.getCoroutineContext();
            }
        });
        updateListPanel();
        return mainPanel;
    }

    private void updateListEmptyText(PatchListSearchValue patchListSearchValue) {
        patchesList.getEmptyText().clear();
        if (loadedRadPatches.isEmpty() || patchModel.isEmpty()) {
            patchesList.getEmptyText().setText(RadicleBundle.message("nothingFound"));
        }
        if (patchListSearchValue.getFilterCount() > 0 && (loadedRadPatches.isEmpty() || patchModel.isEmpty())) {
            patchesList.getEmptyText().appendSecondaryText(RadicleBundle.message("clearFilters"), SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> resetFilters());
        }
    }

    private void resetFilters() {
        searchVm.getSearchState().setValue(new PatchListSearchValue());
    }

    private JPanel createListPanel() {
        patchesList = new JBList<>(patchModel);
        patchesList.setCellRenderer(new PatchListCellRenderer());
        patchesList.setExpandableItemsEnabled(false);
        patchesList.getEmptyText().clear();
        patchesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ScrollingUtil.installActions(patchesList);
        ListUtil.installAutoSelectOnMouseMove(patchesList);
        ListUiUtil.Selection.INSTANCE.installSelectionOnFocus(patchesList);
        ListUiUtil.Selection.INSTANCE.installSelectionOnRightClick(patchesList);
        patchesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != 1 || e.getClickCount() != 2) {
                    return;
                }
                final var selectedPatch = patchesList.getSelectedValue();
                patchModel.clear();
                controller.createPatchProposalPanel(selectedPatch);
            }
        });
        var scrollPane = ScrollPaneFactory.createScrollPane(patchesList, true);
        progressStripe = new ProgressStripe(scrollPane, controller.getDisposer(), ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
        return progressStripe;
    }

    public void filterList(PatchListSearchValue patchListSearchValue) {
        patchModel.clear();
        if (loadedRadPatches == null) {
            return;
        }
        if (patchListSearchValue.getFilterCount() == 0) {
            patchModel.addAll(loadedRadPatches);
        } else {
            var projectFilter = patchListSearchValue.project;
            var searchFilter = patchListSearchValue.searchQuery;
            var peerAuthorFilter = patchListSearchValue.author;
            List<RadPatch> filteredPatches = loadedRadPatches.stream()
                    .filter(p -> searchFilter == null || p.author.id().contains(searchFilter) || p.title.contains(searchFilter))
                    .filter(p -> projectFilter == null || p.repo.getRoot().getName().equals(projectFilter))
                    .filter(p -> peerAuthorFilter == null || p.author.id().contains(peerAuthorFilter))
                    .collect(Collectors.toList());
            patchModel.addAll(filteredPatches);
        }
        updateListEmptyText(patchListSearchValue);
    }

    private List<RadPatch> getPatchProposals(List<GitRepository> repos) {
        var outputs = new ArrayList<RadPatch>();
        var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, true);
        if (radInitializedRepos.isEmpty()) {
            return List.of();
        }
        for (GitRepository repo : radInitializedRepos) {
            var radInspect = new RadInspect(repo);
            var output = radInspect.perform();
            if (RadAction.isSuccess(output)) {
                var radProjectId = output.getStdout().trim();
                var seedNode =  this.radicleProjectSettingsHandler.loadSettings().getSeedNode();
                var patches = myApi.fetchPatches(seedNode, radProjectId, repo);
                if (!patches.isEmpty()) {
                    outputs.addAll(patches);
                }
            }
        }
        return outputs;
    }

    private void updateListPanel() {
        var radPatchesCountDown = new CountDownLatch(1);
        searchVm.setRadPatchesCountDown(radPatchesCountDown);
        var settings =  radicleProjectSettingsHandler.loadSettings();
        var seedNode = settings.getSeedNode();
        if (seedNode == null || Strings.isNullOrEmpty(seedNode.url)) {
            return;
        }
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            patchModel.clear();
            progressStripe.startLoading();
            var patchProposals = getPatchProposals(repos);
            loadedRadPatches = patchProposals;
            searchVm.setRadPatches(loadedRadPatches);
            radPatchesCountDown.countDown();
            patchModel.addAll(patchProposals);
            ApplicationManager.getApplication().invokeLater(() -> {
                progressStripe.stopLoading();
                updateListEmptyText(searchVm.getSearchState().getValue());
            }, ModalityState.any());
        });
    }

    private static class PatchListCellRenderer implements ListCellRenderer<RadPatch> {

        public PatchListCellRenderer() {
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends RadPatch> list, RadPatch value, int index, boolean isSelected, boolean cellHasFocus) {
            var cell = new Cell(index, value);
            cell.setBackground(ListUiUtil.WithTallRow.INSTANCE.background(list, isSelected, list.hasFocus()));
            cell.title.setForeground(ListUiUtil.WithTallRow.INSTANCE.foreground(isSelected, list.hasFocus()));
            return cell;
        }

        public static class Cell extends JPanel {
            public final int index;
            public final JLabel title;
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

                title = new JLabel(patch.title);
                patchPanel.add(title, BorderLayout.NORTH);
                var revision = patch.revisions.get(patch.revisions.size() - 1);
                var date = Date.from(revision.timestamp());
                var formattedDate = new SimpleDateFormat("dd/MM/yyyy").format(date);
                var info = new JLabel("Created : " + formattedDate + " by " + patch.author.id());
                info.setForeground(JBColor.GRAY);
                patchPanel.add(info, BorderLayout.SOUTH);
                add(patchPanel, new CC().minWidth("0").gapAfter("push"));
            }

            @Override
            public AccessibleContext getAccessibleContext() {
                var ac = super.getAccessibleContext();
                ac.setAccessibleName(patch.repo.getRoot().getName() + " - " + patch.author);
                return ac;
            }
        }
    }

    public DefaultListModel<RadPatch> getPatchModel() {
        return patchModel;
    }

    public PatchSearchPanelViewModel getSearchVm() {
        return searchVm;
    }

    public JBList<RadPatch> getPatchesList() {
        return patchesList;
    }
}
