package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.AbstractReviewListSearchValue;

public class IssueListSearchValue extends AbstractReviewListSearchValue {

    public IssueListSearchValue(IssueListSearchValue issueListSearchValue) {
        super(issueListSearchValue.project, issueListSearchValue.searchQuery, issueListSearchValue.author,
                issueListSearchValue.state, issueListSearchValue.tag);
    }

    public IssueListSearchValue() {

    }
}
