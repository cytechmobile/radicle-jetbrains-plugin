package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline;

import com.intellij.CommonBundle;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.CollaborationToolsUIUtilKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTitleUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CommentInputActionsComponentFactory;
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentTextFieldFactory;
import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.collaboration.ui.util.ActionUtilKt;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.ListLayout;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.Function;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPatchEdit;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow;

public class TimelineComponent {
    private static final Logger logger = LoggerFactory.getLogger(TimelineComponent.class);

    private final TimelineComponentFactory componentsFactory;
    private final RadPatch radPatch;
    private final SingleValueModel<RadPatch> radPatchModel;
    private BaseHtmlEditorPane headerTitle;

    public TimelineComponent(SingleValueModel<RadPatch> radPatchModel) {
        this.radPatchModel = radPatchModel;
        this.radPatch = radPatchModel.getValue();
        componentsFactory = new TimelineComponentFactory(radPatch);
    }

    public JComponent create() {
//        var header = getHeader();
        var header = getHeader();
        var descriptionWrapper = new Wrapper();
        descriptionWrapper.setOpaque(false);
        descriptionWrapper.setContent(componentsFactory.createDescSection());

        var timelinePanel = new JPanel(ListLayout.vertical(0, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
        timelinePanel.setBorder(JBUI.Borders.empty(CodeReviewTimelineUIUtil.VERT_PADDING, 0));

        timelinePanel.setOpaque(false);
        timelinePanel.add(header);
        timelinePanel.add(descriptionWrapper);
        timelinePanel.add(componentsFactory.createRevisionSection());

        var mainPanel = new Wrapper();
        var scrollPanel = ScrollPaneFactory.createScrollPane(timelinePanel, true);
        scrollPanel.setOpaque(false);

        mainPanel.setContent(scrollPanel);
        return mainPanel;
    }

    private JComponent getHeader() {
        final var title = CodeReviewTitleUIUtil.INSTANCE.createTitleText(radPatch.title, radPatch.id, "", "");

        headerTitle = new BaseHtmlEditorPane();
        headerTitle.setFont(JBFont.h2().asBold());
        headerTitle.setBody(title);

        var panelHandle = new EditablePanelHandler(radPatch.repo.getProject(), headerTitle,
                RadicleBundle.message("patch.proposal.change.title", "change title"), new SingleValueModel<>(radPatch.title), (editedTitle) -> {
            var patchEdit = new RadPatchEdit(radPatch.repo, radPatch.id, editedTitle);
            var out = patchEdit.perform();
            final boolean success = RadAction.isSuccess(out);
            if (success) {
                radPatchModel.setValue(radPatch);
            }
            return true;
        });
        var contentPanel = panelHandle.panel;
        var actionsPanel = CollaborationToolsUIUtilKt.HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP);
        actionsPanel.add(CodeReviewCommentUIUtil.INSTANCE.createEditButton(e -> {
            panelHandle.showAndFocusEditor();
            return null;
        }));

        var b = new CodeReviewChatItemUIUtil.Builder(CodeReviewChatItemUIUtil.ComponentType.FULL,
                i -> new SingleValueModel<>(RadicleIcons.RADICLE), contentPanel);
        b.withHeader(contentPanel, actionsPanel);
        return b.build();
    }

    public BaseHtmlEditorPane getHeaderTitle() {
        return headerTitle;
    }

    public TimelineComponentFactory getComponentsFactory() {
        return componentsFactory;
    }

    public static class EditablePanelHandler {
        private final Project project;
        private final JComponent paneComponent;
        private final SizeRestrictedSingleComponentLayout editorPaneLayout;
        public final JPanel panel;
        public JComponent editor;
        private final String actionName;
        //todo this needs to become dynamically editable
        private final SingleValueModel<String> content;
        private final Function<String, Boolean> okAction;

        public EditablePanelHandler(
                Project project, JComponent paneComponent, String actionName, SingleValueModel<String> content, Function<String, Boolean> okAction) {
            this.project = project;
            this.paneComponent = paneComponent;
            this.editorPaneLayout = SizeRestrictedSingleComponentLayout.Companion.constant(null, null);
            this.panel = new JPanel(null);
            this.panel.setOpaque(false);
            this.editor = null;
            this.content = content;
            this.actionName = actionName;
            this.okAction = okAction;
            this.hideEditor();
            content.addListener(e -> {
                var displayed = this.editor != null;
                ApplicationManager.getApplication().invokeLater(() -> {
                    this.hideEditor();
                    if (displayed) {
                        this.showAndFocusEditor();
                    }
                });
                return null;
            });
        }

        public void showAndFocusEditor() {
            if (editor == null) {
                /*val model = GHCommentTextFieldModel(project, getSourceText()) { newText ->
                        updateText(newText).successOnEdt {
                    hideEditor()
                }*/

                var submitShortcutText = CommentInputActionsComponentFactory.INSTANCE.getSubmitShortcutText();
                var cancelAction = ActionUtilKt.swingAction(CommonBundle.getCancelButtonText(), (e) -> {
                    logger.warn("cancel action");
                    hideEditor();
                    return null;
                });

                var doc = LanguageTextField.createDocument(content.getValue(), PlainTextLanguage.INSTANCE, project,
                        new LanguageTextField.SimpleDocumentCreator());
                var field = CommentTextFieldFactory.INSTANCE.create(project, doc, CommentTextFieldFactory.ScrollOnChangePolicy.ScrollToField.INSTANCE,
                        content.getValue());
                //CollaborationToolsUIUtil.installValidator(textField, model.errorValue.map { it?.localizedMessage })
                //var inputField = wrapWithProgressOverlay(textField, model.isBusyValue);

                var primaryAction = ActionUtilKt.swingAction(actionName, e -> {
                    logger.warn("primary action");
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var res = okAction.fun(field.getText());
                        if (res) {
                            ApplicationManager.getApplication().invokeLater(this::hideEditor);
                        }
                    });
                    return null;
                });
                var actions = new CommentInputActionsComponentFactory.Config(
                        MutableStateFlow(primaryAction),
                        MutableStateFlow(List.of()), MutableStateFlow(List.of()),
                        MutableStateFlow(cancelAction),
                        MutableStateFlow(RadicleBundle.message("patch.proposal.submit.hint", "{0} to {1}", submitShortcutText, actionName)));

                editor = CommentInputActionsComponentFactory.INSTANCE.attachActions(field, actions);
                panel.remove(paneComponent);

                panel.setLayout(editorPaneLayout);
                panel.add(editor);
                panel.revalidate();
                panel.repaint();
            }

            if (editor != null) {
                CollaborationToolsUIUtil.INSTANCE.focusPanel(editor);
            }
        }

        public void hideEditor() {
            if (editor != null) {
                panel.remove(editor);
            }
            panel.setLayout(new BorderLayout());
            panel.add(paneComponent, BorderLayout.CENTER);
            panel.revalidate();
            panel.repaint();
            editor = null;
        }
    }
}
