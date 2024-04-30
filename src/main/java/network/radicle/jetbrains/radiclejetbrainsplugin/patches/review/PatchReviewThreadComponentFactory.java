package network.radicle.jetbrains.radiclejetbrainsplugin.patches.review;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.ToggleableContainer;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.collaboration.ui.codereview.timeline.thread.TimelineThreadCommentsPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.ThreadModel;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.EditablePanelHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.MarkDownEditorPaneFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PatchReviewThreadComponentFactory {
    private static final String PATTERN_FORMAT = "dd/MM/yyyy HH:mm";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

    private final RadicleProjectApi api;
    private final RadPatch patch;
    private final ObservableThreadModel threadsModel;
    private final RadicleProjectService radicleProjectService;

    public PatchReviewThreadComponentFactory(RadPatch patch, ObservableThreadModel threadsModel) {
        this.radicleProjectService = patch.project.getService(RadicleProjectService.class);
        this.api = patch.project.getService(RadicleProjectApi.class);
        this.threadsModel = threadsModel;
        this.patch = patch;
    }

    public JComponent createThread(ThreadModel threadModel) {
        var verticalPanel = CollaborationToolsUIUtilKt.VerticalListPanel(0);
        CollectionListModel<RadDiscussion> listModel = new CollectionListModel<>();
        for (var discussion : threadModel.getRadDiscussion()) {
            listModel.add(discussion);
        }
        var commentsPanel = new TimelineThreadCommentsPanel<>(listModel, this::createComponent, 0, 10);
        verticalPanel.add(commentsPanel);
        verticalPanel.add(getThreadActionsComponent(threadModel));
        verticalPanel.setBorder(JBUI.Borders.empty(CodeReviewChatItemUIUtil.ComponentType.COMPACT.getInputPaddingInsets()));
        return CodeReviewCommentUIUtil.INSTANCE.createEditorInlayPanel(verticalPanel);
    }

    public boolean deleteComment(RadDiscussion disc) {
        var latestRev = patch.revisions.get(patch.revisions.size() - 1);
        var res = api.deleteRevisionComment(patch, latestRev.id(), disc.id);
        return res != null;
    }

    public JComponent createUncollapsedThreadActionsComponent(ThreadModel threadModel) {
        var panelHandle = new EditablePanelHandler.PanelBuilder(patch.project, new JPanel(),
                RadicleBundle.message("reply", "Reply"),
                new SingleValueModel<>(""), (field) -> {
            var line = Integer.valueOf(threadModel.getLine());
            var latestRev = threadModel.getRadDiscussion().get(threadModel.getRadDiscussion().size() - 1);
            var location = new RadDiscussion.Location(threadsModel.getFilePath(), "ranges",
                    threadsModel.getCommitHash(), line, line);
            var res = this.api.addPatchComment(patch, field.getText(), latestRev.id, location, field.getEmbedList());
            boolean success = res != null;
            if (success) {
                threadsModel.update(patch);
            }
            return success;
        }).enableDragAndDrop(false).hideCancelAction(true).build();
        panelHandle.showAndFocusEditor();
        var builder = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.COMPACT, integer ->
                new SingleValueModel<>(RadicleIcons.DEFAULT_AVATAR), panelHandle.panel);
        return builder.build();
    }

    public JComponent createCollapsedThreadActionComponent(ReplyAction replyAct) {
        var reply = new LinkLabel<Any>(RadicleBundle.message("reply", "Reply"), null) {
            @Override
            public void doClick() {
                replyAct.reply();
            }
        };
        var horizontalPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        horizontalPanel.setBorder(JBUI.Borders.emptyLeft(CodeReviewChatItemUIUtil.ComponentType.COMPACT.getContentLeftShift() + 12));
        horizontalPanel.add(reply);
        return horizontalPanel;
    }

    public JComponent getThreadActionsComponent(ThreadModel myThreadsModel) {
        var toggleModel = new SingleValueModel<>(false);
        return ToggleableContainer.INSTANCE.create(toggleModel, () -> {
            ReplyAction rep = () -> {
                toggleModel.setValue(true);
            };
            return createCollapsedThreadActionComponent(rep);
        }, () -> createUncollapsedThreadActionsComponent(myThreadsModel));
    }

    private JComponent createComponent(RadDiscussion disc) {
        var editorPane = new MarkDownEditorPaneFactory(disc.body, patch.project, patch.radProject.id, patch.repo.getRoot());
        var panelHandle = new EditablePanelHandler.PanelBuilder(patch.project, editorPane.htmlEditorPane(),
                RadicleBundle.message("review.edit.comment"),
                new SingleValueModel<>(disc.body), (field) -> {
            var latestRev = patch.revisions.get(patch.revisions.size() - 1);
            var res = this.api.changePatchComment(latestRev.id(), disc.id, field.getText(), patch, List.of());
            boolean success = res != null;
            if (success) {
                threadsModel.update(patch);
            }
            return success;
        }).enableDragAndDrop(false).build();
        var editButton = CodeReviewCommentUIUtil.INSTANCE.createEditButton(actionEvent -> {
            panelHandle.showAndFocusEditor();
            return Unit.INSTANCE;
        });
        var deleteButton = CodeReviewCommentUIUtil.INSTANCE.createDeleteCommentIconButton(actionEvent -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var success = this.deleteComment(disc);
                if (success) {
                    threadsModel.update(patch);
                }
            });
            return Unit.INSTANCE;
        });
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        var radDetails = this.radicleProjectService.getRadDetails();
        var self = radDetails != null && radDetails.did.equals(disc.author.id);
        if (self) {
            actionsPanel.add(editButton);
            actionsPanel.add(deleteButton);
        }
        var builder = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.COMPACT, integer ->
                new SingleValueModel<>(RadicleIcons.DEFAULT_AVATAR), panelHandle.panel);
        var author = !Strings.isNullOrEmpty(disc.author.alias) ? disc.author.alias : Utils.formatDid(disc.author.id);
        var authorLink = HtmlChunk.link("#",
                author).wrapWith(HtmlChunk.font(ColorUtil.toHtmlColor(UIUtil.getLabelForeground()))).bold();
        var titleText = new HtmlBuilder().append(authorLink)
                .append(HtmlChunk.nbsp())
                .append(disc.timestamp != null ? DATE_TIME_FORMATTER.format(disc.timestamp) : "");
        builder.withHeader(new JLabel(MarkDownEditorPaneFactory.wrapHtml(titleText.toString())), actionsPanel);
        return builder.build();
    }

    public interface ReplyAction {
        void reply();
    }
}
