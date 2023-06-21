package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBList;
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.util.ui.UIUtil;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class IssueFilterPanel extends ReviewListSearchPanelFactory<IssueListSearchValue,
        IssueSearchPanelViewModel.IssueListQuickFilter, IssueSearchPanelViewModel> {

    private static final Logger logger = LoggerFactory.getLogger(IssueFilterPanel.class);
    private final IssueSearchPanelViewModel viewModel;
    public IssueFilterPanel(@NotNull IssueSearchPanelViewModel issueSearchPanelViewModel) {
        super(issueSearchPanelViewModel);
        this.viewModel = issueSearchPanelViewModel;
    }

    @NotNull
    @Override
    protected List<JComponent> createFilters(@NotNull CoroutineScope coroutineScope) {
        var stateFilter = new DropDownComponentFactory<>(this.viewModel.stateFilter()).create(coroutineScope, RadicleBundle.message("state"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> ChooserPopupUtil.INSTANCE.showAsyncChooserPopup(relativePoint, jbPopupPopupState,
                        continuation1 -> Arrays.stream(RadIssue.State.values()).map(e -> e.status).collect(Collectors.toList()), state ->
                                new ChooserPopupUtil.PopupItemPresentation.Simple((String) state, null, null), continuation));

        var projectFilter = new DropDownComponentFactory<>(this.viewModel.projectFilterState()).create(coroutineScope, RadicleBundle.message("project"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = createPopup(viewModel.getProjectNames());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        var authorFilter = new DropDownComponentFactory<>(this.viewModel.authorFilterState()).create(coroutineScope, RadicleBundle.message("author"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = createPopup(viewModel.getAuthors());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        var tagFilter = new DropDownComponentFactory<>(this.viewModel.tagFilter()).create(coroutineScope, RadicleBundle.message("tag"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = createPopup(viewModel.getTags());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        var assigneesFilter = new DropDownComponentFactory<>(this.viewModel.assigneeFilter()).create(coroutineScope, RadicleBundle.message("assignees"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = createPopup(viewModel.getAssignees());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });


        return List.of(stateFilter, projectFilter, authorFilter, assigneesFilter, tagFilter);
    }

    @NotNull
    @Override
    protected String getShortText(@NotNull IssueListSearchValue issueListSearchValue) {
        return "";
    }

    @NotNull
    @Override
    protected String getQuickFilterTitle(@NotNull IssueSearchPanelViewModel.IssueListQuickFilter issueListQuickFilter) {
        return Objects.requireNonNullElse(issueListQuickFilter.getFilter().state, "");
    }

    private JBPopup createPopup(CompletableFuture<List<String>> list) {
        return JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>())
                .setResizable(true)
                .setMovable(true)
                .setFilterAlwaysVisible(true)
                .setRenderer(new ChooserPopupUtil.SimplePopupItemRenderer<>(s ->
                        new ChooserPopupUtil.PopupItemPresentation.Simple(s, null, null)))
                .setNamerForFiltering(s -> s)
                .addListener(new JBPopupListener() {
                    @Override
                    public void beforeShown(@NotNull LightweightWindowEvent event) {
                        //Get popup
                        var popUp = event.asPopup().getContent();
                        var jbList = UIUtil.findComponentOfType(popUp, JBList.class);
                        if (jbList == null) {
                            return;
                        }
                        //Find model
                        var listModel = (NameFilteringListModel<String>) jbList.getModel();
                        //Start loading indicator
                        jbList.setPaintBusy(true);
                        // Get a single thread for our operation
                        var executor = Executors.newSingleThreadExecutor();
                        executor.execute(() -> {
                            try {
                                //Wait for the data to be ready
                                var isFinished = viewModel.getCountDown().await(5, TimeUnit.SECONDS);
                                if (!isFinished) {
                                    return;
                                }
                                var data = list.get(5, TimeUnit.SECONDS);
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    // Stop loading indicator
                                    jbList.setPaintBusy(false);
                                    // Update the model with the new data
                                    listModel.replaceAll(data);
                                    event.asPopup().pack(true, true);
                                }, ModalityState.any());
                            } catch (Exception e) {
                                logger.warn("Unable to load filters");
                                ApplicationManager.getApplication().invokeLater(() ->
                                        jbList.setPaintBusy(false), ModalityState.any());
                            }
                        });
                    }
                }).createPopup();
    }
}
