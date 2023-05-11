package network.radicle.jetbrains.radiclejetbrainsplugin.patches.overview;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageComponentFactory;
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageType;
import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.components.panels.ListLayout;
import com.intellij.util.ui.UIUtil;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PatchTimelineFactory {
    private static final Logger logger = LoggerFactory.getLogger(PatchTimelineFactory.class);
    private static final String PATTERN_FORMAT = "dd/MM/yyyy hh:mm";
    private static final DateTimeFormatter dateTimeformatter =
            DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());
    private static final String COMMIT_HASH = "commit://";
    private final RadPatch patch;
    private JComponent descSection;
    private JComponent commentSection;
    private final CountDownLatch latch = new CountDownLatch(1);
    private Map<String, List<GitCommit>> groupedCommits;

    public PatchTimelineFactory(RadPatch radPatch) {
        this.patch = radPatch;
    }

    protected Map<String, List<GitCommit>> groupPatchCommits() {
        var revisions = new HashMap<String, List<GitCommit>>();
        try {
            for (var rev : patch.revisions) {
                var patchCommits = GitHistoryUtils.history(patch.repo.getProject(),
                        patch.repo.getRoot(), rev.base() + "..." + rev.oid());
                revisions.put(rev.id(), patchCommits);
            }
            return revisions;
        } catch (Exception e) {
          //  logger.warn("error calculating patch commits for patch: {}", patch, e);
          // RadAction.showErrorNotification(patch.repo.getProject(),
          //         RadicleBundle.message("radCliError"),
          //         RadicleBundle.message("errorCalculatingPatchProposalCommits"));
        }
        return null;
    }

    public JComponent createDescSection() {
        var contentPanel = new JPanel(SizeRestrictedSingleComponentLayout.Companion.constant(null, null));
        var textHtmlEditor = new BaseHtmlEditorPane();
        textHtmlEditor.setBody(patch.description);
        contentPanel.add(textHtmlEditor);
        contentPanel.setOpaque(false);
        //TODO implement edit functionality here
        var editButton = CodeReviewCommentUIUtil.INSTANCE.createEditButton(actionEvent -> null);
        var horizontalPanel = getHorizontalPanel(8);
        horizontalPanel.add(editButton);
        horizontalPanel.setOpaque(false);
        descSection = createTimeLineItem(contentPanel, horizontalPanel, patch.author.id(), null);
        return descSection;
    }

    public JComponent createRevisionSection() {
        var mainPanel = getVerticalPanel(0);
        var loadingIcon = new JLabel(new AnimatedIcon.Default());
        //TODO fix icon position
        mainPanel.add(loadingIcon);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            groupedCommits = groupPatchCommits();
            latch.countDown();
            ApplicationManager.getApplication().invokeLater(() -> {
                loadingIcon.setVisible(false);
                if (groupedCommits == null) {
                    //TODO handle error message
                    return;
                }
                for (var rev : patch.revisions) {
                    var contentPanel = getVerticalPanel(4);
                    var textHtmlEditor = new BaseHtmlEditorPane();
                    textHtmlEditor.setBody(rev.description());
                    contentPanel.add(textHtmlEditor);
                    var patchCommits = groupedCommits.get(rev.id());
                    contentPanel.add(createCommitsSection(patchCommits));
                    contentPanel.setOpaque(false);
                    var horizontalPanel = getHorizontalPanel(8);
                    horizontalPanel.setOpaque(false);
                    var item = createTimeLineItem(contentPanel, horizontalPanel, "Revision " + rev.id() + " was published",
                            rev.timestamp());
                    mainPanel.add(item);
                }
            });
        });
        return mainPanel;
    }

    public JComponent createCommentSection() {
        var mainPanel = getVerticalPanel(0);
        for (var rev : patch.revisions) {
            for (var com : rev.discussions()) {
                var textHtmlEditor = new BaseHtmlEditorPane();
                textHtmlEditor.setOpaque(false);
                textHtmlEditor.setBody(com.body());
                var horizontalPanel = getHorizontalPanel(8);
                //TODO implement edit functionality here
                var editButton = CodeReviewCommentUIUtil.INSTANCE.createEditButton(actionEvent -> null);
                horizontalPanel.add(editButton);
                horizontalPanel.setOpaque(false);
                var contentPanel = new JPanel(SizeRestrictedSingleComponentLayout.Companion.constant(null, null));
                contentPanel.setOpaque(false);
                contentPanel.add(StatusMessageComponentFactory.INSTANCE.create(textHtmlEditor, StatusMessageType.WARNING));
                mainPanel.add(createTimeLineItem(contentPanel, horizontalPanel, com.author().id(), com.timestamp()));
            }
        }
        commentSection = mainPanel;
        return mainPanel;
    }

    public BaseHtmlEditorPane createCommitsSection(List<GitCommit> commits) {
        var builder = new HtmlBuilder();
        for (var commit : commits) {
            builder.append(HtmlChunk.p()
                    .children(HtmlChunk.link(COMMIT_HASH + commit.getId(), commit.getId().asString()),
                            HtmlChunk.nbsp(),
                            HtmlChunk.raw(commit.getFullMessage())));
            builder.append(HtmlChunk.link(commit.getAuthor().getName(), commit.getAuthor().getName()));
            builder.append(HtmlChunk.nbsp());
            builder.append(dateTimeformatter.format(Instant.ofEpochMilli(commit.getAuthorTime())));
        }
        var commitEditor = new BaseHtmlEditorPane();
        commitEditor.setBody(builder.toString());
        return commitEditor;
    }

    private JComponent createTimeLineItem(JComponent contentPanel,
                                          JComponent actionsPanel, String title, Instant date) {
        var authorDid = HtmlChunk.link("",
                title).wrapWith(HtmlChunk.font(ColorUtil.toHtmlColor(UIUtil.getLabelForeground()))).bold();
        var titleText = new HtmlBuilder().append(authorDid)
                .append(HtmlChunk.nbsp())
                .append(date != null ? dateTimeformatter.format(date) : "");
        var titleTextPane = new BaseHtmlEditorPane();
        titleTextPane.setOpaque(false);
        titleTextPane.setBody(titleText.toString());
        titleTextPane.setForeground(UIUtil.getContextHelpForeground());
        return new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                integer -> new SingleValueModel<>(RadicleIcons.RADICLE), contentPanel)
                .withHeader(titleTextPane, actionsPanel).build();
    }

    private JComponent getVerticalPanel(int gap) {
        return new JPanel(ListLayout.vertical(gap, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
    }

    private JComponent getHorizontalPanel(int gap) {
        return new JPanel(ListLayout.horizontal(gap, ListLayout.Alignment.START, ListLayout.GrowPolicy.GROW));
    }

    public JComponent getDescSection() {
        return descSection;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public Map<String, List<GitCommit>> getGroupedCommits() {
        return groupedCommits;
    }

    public JComponent getCommentSection() {
        return commentSection;
    }
}
