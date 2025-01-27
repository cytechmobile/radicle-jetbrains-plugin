package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

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
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ListPanel;

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

public class IssueListPanel extends ListPanel<RadIssue, IssueListSearchValue, IssueSearchPanelViewModel> {
    private final ListCellRenderer<RadIssue> issueListCellRenderer = new IssueListCellRenderer();
    private final IssueTabController cntrl;
    protected IssueListSearchValue issueListSearchValue;
    protected RadicleCliService rad;

    public IssueListPanel(IssueTabController controller, Project project) {
        super(controller, project);
        this.rad = project.getService(RadicleCliService.class);
        this.cntrl = controller;
        this.issueListSearchValue = getEmptySearchValueModel();
        this.issueListSearchValue.state = RadIssue.State.OPEN.label;
    }

    @Override
    public List<RadIssue> fetchData(String projectId, GitRepository repo) {
        var cliService = repo.getProject().getService(RadicleCliService.class);
        var issues = cliService.getIssues(repo, projectId);
        if (issues != null && !issues.isEmpty()) {
            for (var issue : issues) {
                issue.author.tryResolveAlias(rad);
                if (issue.assignees != null && !issue.assignees.isEmpty()) {
                    for (var a : issue.assignees) {
                        a.tryResolveAlias(rad);
                    }
                }
            }
        }
        return issues;
    }

    @Override
    public ListCellRenderer<RadIssue> getCellRenderer() {
        return issueListCellRenderer;
    }

    @Override
    public IssueSearchPanelViewModel getViewModel(CoroutineScope scope) {
        var model = new IssueSearchPanelViewModel(scope, new IssueSearchHistoryModel(), project);
        model.getSearchState().setValue(this.issueListSearchValue);
        return model;
    }

    @Override
    public void onItemClick(RadIssue obj) {
        this.cntrl.createIssuePanel(obj);
    }

    @Override
    public JComponent getFilterPanel(IssueSearchPanelViewModel searchViewModel, CoroutineScope scope) {
        return new IssueFilterPanel(searchViewModel).create(scope);
    }

    @Override
    public void filterList(IssueListSearchValue searchValue) {
        this.issueListSearchValue = searchValue;
        model.clear();
        if (loadedData == null) {
            return;
        }
        if (searchValue.getFilterCount() == 0) {
            model.addAll(loadedData);
        } else {
            var projectFilter = searchValue.project;
            var searchFilter = searchValue.searchQuery;
            var peerAuthorFilter = searchValue.author;
            var stateFilter = searchValue.state;
            var labelFilter = searchValue.label;
            var assigneeFilter = searchValue.assignee;
            var loadedRadIssues = loadedData;
            List<RadIssue> filteredPatches = loadedRadIssues.stream()
                    .filter(p -> Strings.isNullOrEmpty(searchFilter) || p.author.contains(rad, searchFilter) ||
                                 p.title.contains(searchFilter))
                    .filter(p -> Strings.isNullOrEmpty(projectFilter) || p.repo.getRoot().getName().equals(projectFilter))
                    .filter(p -> Strings.isNullOrEmpty(peerAuthorFilter) || p.author.contains(rad, peerAuthorFilter))
                    .filter(p -> Strings.isNullOrEmpty(stateFilter) || (p.state != null && p.state.label.equals(stateFilter)))
                    .filter(p -> Strings.isNullOrEmpty(labelFilter) || p.labels.stream().anyMatch(label -> label.equals(labelFilter)))
                    .filter(p -> Strings.isNullOrEmpty(assigneeFilter) || p.assignees.stream().anyMatch(assignee ->
                            assignee.contains(rad, assigneeFilter)))
                    .collect(Collectors.toList());
            model.addAll(filteredPatches);
        }
    }

    @Override
    public IssueListSearchValue getEmptySearchValueModel() {
        return new IssueListSearchValue();
    }

    public static class IssueListCellRenderer implements ListCellRenderer<RadIssue> {

        public IssueListCellRenderer() {
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends RadIssue> list, RadIssue value, int index, boolean isSelected, boolean cellHasFocus) {
            var cell = new IssueListPanel.IssueListCellRenderer.Cell(index, value);
            cell.setBackground(ListUiUtil.WithTallRow.INSTANCE.background(list, isSelected, list.hasFocus()));
            cell.title.setForeground(ListUiUtil.WithTallRow.INSTANCE.foreground(isSelected, list.hasFocus()));
            return cell;
        }

        public static class Cell extends JPanel {
            public final int index;
            public final JLabel title;
            public final RadIssue issue;

            public Cell(int index, RadIssue issue) {
                this.index = index;
                this.issue = issue;
                var rad = issue.project.getService(RadicleCliService.class);

                var gapAfter = JBUI.scale(5);
                var issuePanel = new JPanel();
                issuePanel.setOpaque(false);
                issuePanel.setBorder(JBUI.Borders.empty(10, 8));
                issuePanel.setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0")
                        .insets("0", "0", "0", "0")
                        .fillX()));

                setLayout(new MigLayout(new LC().gridGap(gapAfter + "px", "0").noGrid()
                        .insets("0", "0", "0", "0")
                        .fillX()));

                var innerPanel = new JPanel();
                innerPanel.setLayout(new BorderLayout());

                title = new JLabel(issue.title);
                issuePanel.add(title, BorderLayout.NORTH);
                if (!issue.discussion.isEmpty()) {
                    var firstDiscussion = issue.discussion.get(0);
                    var formattedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(firstDiscussion.timestamp.atZone(ZoneId.systemDefault()));
                    var info = new JLabel(RadicleBundle.message("created") + ": " + formattedDate + " " +
                            RadicleBundle.message("by") + " " + issue.author.generateLabelText(rad));
                    info.setForeground(JBColor.GRAY);
                    if (!issue.labels.isEmpty()) {
                        var labels = new JLabel(RadicleBundle.message("labels") + ": " + String.join(", ", issue.labels));
                        labels.setForeground(JBColor.GRAY);
                        issuePanel.add(labels, BorderLayout.SOUTH);
                    }
                    issuePanel.add(info, BorderLayout.SOUTH);
                }
                add(issuePanel, new CC().minWidth("0").gapAfter("push"));
            }

            @Override
            public AccessibleContext getAccessibleContext() {
                var ac = super.getAccessibleContext();
                ac.setAccessibleName(issue.repo.getRoot().getName());
                return ac;
            }
        }
    }
}
