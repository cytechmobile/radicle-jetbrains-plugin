package network.radicle.jetbrains.radiclejetbrainsplugin.issues;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.AbstractReviewListSearchValue;

public class IssueListSearchValue extends AbstractReviewListSearchValue {
    public String assignee;

    public IssueListSearchValue(IssueListSearchValue searchValue) {
        super(searchValue.project, searchValue.searchQuery, searchValue.author,
                searchValue.state, searchValue.tag);
        this.assignee = searchValue.assignee;
    }

    public IssueListSearchValue() {

    }

    public String getAssignee() {
        return assignee;
    }

    @Override
    public int getFilterCount() {
        var count = super.getFilterCount();
        if (assignee != null) {
            count++;
        }
        return count;
    }
}
