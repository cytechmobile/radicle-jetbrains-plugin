package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline;

import com.google.common.base.Strings;
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
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchUtils;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class TimelineComponentFactory {
    private static final String PATTERN_FORMAT = "dd/MM/yyyy HH:mm";
    private static final String COMMIT_HASH = "commit://";
    private final DateTimeFormatter dateTimeformatter =
            DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());
    private final RadPatch patch;
    private final CountDownLatch latch = new CountDownLatch(1);
    private JComponent descSection;
    private Map<String, List<GitCommit>> groupedCommits;

    public TimelineComponentFactory(RadPatch radPatch) {
        this.patch = radPatch;
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
        mainPanel.add(loadingIcon);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var patchUtils = new PatchUtils(patch);
            groupedCommits = patchUtils.calculateCommits();
            latch.countDown();
            ApplicationManager.getApplication().invokeLater(() -> {
                loadingIcon.setVisible(false);
                if (groupedCommits == null) {
                    RadAction.showErrorNotification(patch.repo.getProject(),
                            RadicleBundle.message("radCliError"),
                            RadicleBundle.message("errorCalculatingPatchProposalCommits"));
                    return;
                }
                for (var rev : patch.revisions) {
                    var contentPanel = getVerticalPanel(4);
                    var textHtmlEditor = new BaseHtmlEditorPane();
                    var description = !Strings.isNullOrEmpty(rev.description()) ? rev.description() :
                            RadicleBundle.message("noDescription");
                    textHtmlEditor.setBody(description);
                    contentPanel.add(textHtmlEditor);
                    var patchCommits = groupedCommits.get(rev.id());
                    Collections.reverse(patchCommits);
                    contentPanel.add(StatusMessageComponentFactory.INSTANCE.create(createCommitsSection(patchCommits), StatusMessageType.INFO));
                    contentPanel.setOpaque(false);
                    var horizontalPanel = getHorizontalPanel(8);
                    horizontalPanel.setOpaque(false);
                    var item = createTimeLineItem(contentPanel, horizontalPanel, "Revision " + rev.id() + " was published",
                            rev.timestamp());
                    mainPanel.add(item);
                    mainPanel.add(createCommentSection(List.of(rev)));
                }
            });
        });
        return mainPanel;
    }

    private String findMessage(String replyTo) {
        for (var rev : patch.revisions) {
            for (var com : rev.discussions()) {
                if (com.id().equals(replyTo)) {
                    return com.body();
                }
            }
        }
        return "";
    }

    private JComponent createCommentSection(List<RadPatch.Revision> revisions) {
        var mainPanel = getVerticalPanel(0);
        for (var rev : revisions) {
            for (var com : rev.discussions()) {
                var textHtmlEditor = new BaseHtmlEditorPane();
                textHtmlEditor.setOpaque(false);
                var message = com.body();
                if (!Strings.isNullOrEmpty(com.replyTo())) {
                    var replyToMessage = findMessage(com.replyTo());
                    message = "<div><div style=\"border-left: 2px solid black;\">" +
                            " <div style=\"margin-left:10px\">" + replyToMessage + "</div>\n" +
                            "</div><div style=\"margin-top:5px\">" + message + "</div></div>";
                }
                textHtmlEditor.setBody("<html><body>" + message + "</body></html>");
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
}