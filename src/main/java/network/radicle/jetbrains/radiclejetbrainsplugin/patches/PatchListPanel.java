package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListUiUtil;
import git4idea.repo.GitRepository;
import kotlinx.coroutines.CoroutineScope;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ListPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

public class PatchListPanel extends ListPanel<RadPatch, PatchListSearchValue, PatchSearchPanelViewModel> {
    private final PatchTabController controller;
    private final ListCellRenderer<RadPatch> patchListCellRenderer = new PatchListCellRenderer();
    protected PatchListSearchValue patchListSearchValue;

    public PatchListPanel(PatchTabController ctrl, Project project) {
        super(ctrl, project);
        this.controller = ctrl;
        this.patchListSearchValue = getEmptySearchValueModel();
        this.patchListSearchValue.state = RadPatch.State.OPEN.label;
    }

    @Override
    public List<RadPatch> fetchData(String projectId, GitRepository repo) {
       var cliService = repo.getProject().getService(RadicleCliService.class);
       return cliService.getPatches(repo, projectId);
    }

    @Override
    public ListCellRenderer<RadPatch> getCellRenderer() {
         return patchListCellRenderer;
    }

    @Override
    public PatchSearchPanelViewModel getViewModel(CoroutineScope scope) {
        var model = new PatchSearchPanelViewModel(scope, new PatchSearchHistoryModel(), project);
        model.getSearchState().setValue(this.patchListSearchValue);
        return model;
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
     public void filterList(PatchListSearchValue plsv) {
        this.patchListSearchValue = plsv;
         model.clear();
         if (loadedData == null) {
             return;
         }
         if (plsv.getFilterCount() == 0) {
             model.addAll(loadedData);
             return;
         }
         var projectFilter = plsv.project;
         var searchFilter = plsv.searchQuery;
         var peerAuthorFilter = plsv.author;
         var stateFilter = plsv.state;
         var labelFilter = plsv.label;
         var loadedRadPatches = loadedData;
         var patchesStream = loadedRadPatches.stream();
         if (!Strings.isNullOrEmpty(plsv.searchQuery)) {
             final var search = plsv.searchQuery.toLowerCase().trim();
             patchesStream = patchesStream.filter(p ->
                     p.id.toLowerCase().contains(search) ||
                     Strings.nullToEmpty(p.author.id).toLowerCase().contains(search) ||
                     Strings.nullToEmpty(p.author.alias).toLowerCase().contains(search) ||
                     Strings.nullToEmpty(p.title).toLowerCase().contains(searchFilter.toLowerCase().trim()) ||
                     Strings.nullToEmpty(p.getLatestNonEmptyRevisionDescription()).toLowerCase().contains(search));
         }
         if (!Strings.isNullOrEmpty(plsv.project)) {
             patchesStream = patchesStream.filter(p -> p.repo.getRoot().getName().equals(projectFilter));
         }
         if (!Strings.isNullOrEmpty(peerAuthorFilter)) {
             patchesStream = patchesStream.filter(p -> Strings.nullToEmpty(p.author.alias).equals(peerAuthorFilter) ||
                     p.author.id.equals(peerAuthorFilter));
         }
         if (!Strings.isNullOrEmpty(stateFilter)) {
             patchesStream = patchesStream.filter(p -> p.state != null && p.state.label.equals(stateFilter));
         }
         if (!Strings.isNullOrEmpty(labelFilter)) {
             patchesStream = patchesStream.filter(p -> p.labels.stream().anyMatch(l ->
                     Strings.nullToEmpty(l).toLowerCase().contains(labelFilter.toLowerCase().trim())));
         }
         List<RadPatch> filteredPatches = patchesStream.collect(Collectors.toList());
         model.addAll(filteredPatches);
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
            cell.setToolTipText("<p>" + RadicleBundle.message("patchId") + ": " + cell.patch.id + "</p>" +
                    "<p>" + RadicleBundle.message("created") + " " + RadicleBundle.message("by") + ": " + cell.patch.author.id + "</p>");
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
                patchPanel.setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0").insets("0", "0", "0", "0").fillX()));
                setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0").noGrid().insets("0", "0", "0", "0").fillX()));

                title = new JLabel(patch.title);
                patchPanel.add(title, BorderLayout.NORTH);
                final var infoPanel = new JPanel();
                infoPanel.setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0").insets("0", "0", "0", "0").fillX()));
                infoPanel.setForeground(JBColor.GRAY);
                infoPanel.setOpaque(false);

                var patchId = new JLabel(Utils.formatId(patch.id));
                patchId.setForeground(JBColor.GRAY);
                infoPanel.add(patchId);

                final var revision = patch.revisions.get(patch.revisions.size() - 1);
                final var formattedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(revision.timestamp().atZone(ZoneId.systemDefault()));
                var info = new JLabel(RadicleBundle.message("created") + ": " + formattedDate + " " + RadicleBundle.message("by"));
                info.setForeground(JBColor.GRAY);
                infoPanel.add(info);

                var authorLabel = new JLabel(Strings.isNullOrEmpty(patch.author.alias) ? patch.author.generateLabelText() : patch.author.alias);
                // TODO cannot enable tooltip specifically on author
                authorLabel.setForeground(JBColor.GRAY);
                infoPanel.add(authorLabel);
                if (patch.state != RadPatch.State.OPEN) {
                    var stateLabel = new JLabel(patch.state.label);
                    infoPanel.add(stateLabel);
                }
                patchPanel.add(infoPanel, BorderLayout.SOUTH);
                add(patchPanel, new CC().minWidth("0").gapAfter("push"));
            }

            @Override
            public AccessibleContext getAccessibleContext() {
                var ac = super.getAccessibleContext();
                ac.setAccessibleName(patch.repo.getRoot().getName() + " - " + patch.author.generateLabelText());
                return ac;
            }
        }
    }
}
