package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListUiUtil;
import git4idea.repo.GitRepository;
import kotlinx.coroutines.CoroutineScope;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ListPanel;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class PatchListPanel extends ListPanel<RadPatch, PatchListSearchValue, PatchSearchPanelViewModel> {
    private final PatchTabController controller;
    private final ListCellRenderer<RadPatch> patchListCellRenderer = new PatchListCellRenderer();

    public PatchListPanel(PatchTabController ctrl, Project project) {
        super(ctrl, project);
        this.controller = ctrl;
    }

    @Override
    public List<RadPatch> fetchData(String projectId, GitRepository repo) {
        return api.fetchPatches(projectId, repo);
    }

    @Override
    public ListCellRenderer<RadPatch> getCellRenderer() {
         return patchListCellRenderer;
    }

    @Override
    public PatchSearchPanelViewModel getViewModel(CoroutineScope scope) {
        return new PatchSearchPanelViewModel(scope, new PatchSearchHistoryModel(), project);
    }

    @Override
    public void onItemClick(RadPatch ob) {
        controller.createPatchProposalPanel(ob);
    }

    @Override
    public JComponent getFilterPanel(PatchSearchPanelViewModel searchVm, CoroutineScope scope) {
        return new PatchFilterPanel(searchVm).create(scope);
    }

     @Override
     public void filterList(PatchListSearchValue patchListSearchValue) {
         model.clear();
         if (loadedData == null) {
             return;
         }
         if (patchListSearchValue.getFilterCount() == 0) {
             model.addAll(loadedData);
         } else {
             var projectFilter = patchListSearchValue.project;
             var searchFilter = patchListSearchValue.searchQuery;
             var peerAuthorFilter = patchListSearchValue.author;
             var stateFilter = patchListSearchValue.state;
             var tagFilter = patchListSearchValue.tag;
             var loadedRadPatches = loadedData;
             List<RadPatch> filteredPatches = loadedRadPatches.stream()
                     .filter(p -> searchFilter == null || p.author.id.contains(searchFilter) ||
                             p.title.contains(searchFilter) || p.description.contains(searchFilter))
                     .filter(p -> projectFilter == null || p.repo.getRoot().getName().equals(projectFilter))
                     .filter(p -> peerAuthorFilter == null || p.author.id.equals(peerAuthorFilter))
                     .filter(p -> stateFilter == null || (p.state != null && p.state.status.equals(stateFilter)))
                     .filter(p -> tagFilter == null || p.tags.stream().anyMatch(tag -> tag.equals(tagFilter)))
                     .collect(Collectors.toList());
             model.addAll(filteredPatches);
         }
     }

    @Override
    public PatchListSearchValue getEmptySearchValueModel() {
        return new PatchListSearchValue();
    }

    public static class PatchListCellRenderer implements ListCellRenderer<RadPatch> {

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
                var info = new JLabel("Created : " + formattedDate + " by " + patch.author.id);
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
}
