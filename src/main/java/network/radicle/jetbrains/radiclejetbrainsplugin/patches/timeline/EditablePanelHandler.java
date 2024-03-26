package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline;

import com.intellij.CommonBundle;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.comment.CommentInputActionsComponentFactory;
import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.collaboration.ui.util.ActionUtilKt;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.Function;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.DragAndDropField;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow;

public class EditablePanelHandler {
    private static final Logger logger = LoggerFactory.getLogger(TimelineComponent.class);

    private final Project project;
    private final JComponent paneComponent;
    private final SizeRestrictedSingleComponentLayout editorPaneLayout;
    public final JPanel panel;
    public JComponent editor;
    private final String actionName;
    //todo this needs to become dynamically editable
    private final SingleValueModel<String> content;
    private final Function<DragAndDropField, Boolean> okAction;
    private final boolean hideCancelAction;
    private final boolean closeEditorAfterSubmit;
    private final boolean allowDragAndDrop;
    private DragAndDropField dragAndDropField;
    private SingleValueModel<Boolean> isLoading = new SingleValueModel<>(false);

    public EditablePanelHandler(PanelBuilder builder) {
        this.closeEditorAfterSubmit = builder.closeEditorAfterSubmit;
        this.hideCancelAction = builder.hideCancelAction;
        this.allowDragAndDrop = builder.allowDragAndDrop;
        this.project = builder.project;
        this.paneComponent = builder.paneComponent;
        this.editorPaneLayout = SizeRestrictedSingleComponentLayout.Companion.constant(null, null);
        this.panel = new JPanel(null);
        this.panel.setOpaque(false);
        this.editor = null;
        this.content = builder.content;
        this.actionName = builder.actionName;
        this.okAction = builder.okAction;
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
            dragAndDropField = new DragAndDropField(project, this.allowDragAndDrop);
            dragAndDropField.setText(content.getValue());
            //CollaborationToolsUIUtil.installValidator(textField, model.errorValue.map { it?.localizedMessage })
            //var inputField = wrapWithProgressOverlay(textField, model.isBusyValue);

            var prAction = ActionUtilKt.swingAction(actionName, e -> {
                dragAndDropField.setEnabled(false);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    this.isLoading.setValue(true);
                    var res = okAction.fun(dragAndDropField);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        this.isLoading.setValue(false);
                        dragAndDropField.setEnabled(true);
                        if (res && closeEditorAfterSubmit) {
                            this.hideEditor();
                        }
                    });
                });
                return null;
            });
            dragAndDropField.addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(com.intellij.openapi.editor.event.@NotNull DocumentEvent event) {
                    var enable = !dragAndDropField.getText().isEmpty();
                    prAction.setEnabled(enable);
                }
            });

            var actions = new CommentInputActionsComponentFactory.Config(
                    MutableStateFlow(prAction),
                    MutableStateFlow(List.of()), MutableStateFlow(List.of()),
                    MutableStateFlow(!hideCancelAction ? cancelAction : null),
                    MutableStateFlow(RadicleBundle.message("patch.proposal.submit.hint", submitShortcutText, actionName)));

            if (ApplicationManager.getApplication().isUnitTestMode()) {
                editor = CommentInputActionsComponentFactory.INSTANCE.attachActions(dragAndDropField, actions);
            } else {
                var inputField = CollaborationToolsUIUtil.INSTANCE.wrapWithProgressOverlay(dragAndDropField, isLoading);
                editor = CommentInputActionsComponentFactory.INSTANCE.attachActions(inputField, actions);
            }
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

    public static class PanelBuilder {
        private final Project project;
        private final JComponent paneComponent;
        private final String actionName;
        private final SingleValueModel<String> content;
        private final Function<DragAndDropField, Boolean> okAction;
        private boolean hideCancelAction = false;
        private boolean closeEditorAfterSubmit = true;
        private boolean allowDragAndDrop = true;

        public PanelBuilder(Project project, JComponent paneComponent, String actionName,
                            SingleValueModel<String> content, Function<DragAndDropField, Boolean> okAction) {
            this.project = project;
            this.paneComponent = paneComponent;
            this.actionName = actionName;
            this.content = content;
            this.okAction = okAction;
        }

        public PanelBuilder enableDragAndDrop(boolean allow) {
            this.allowDragAndDrop = allow;
            return this;
        }

        public PanelBuilder hideCancelAction(boolean hide) {
            this.hideCancelAction = hide;
            return this;
        }

        public PanelBuilder closeEditorAfterSubmit(boolean close) {
            this.closeEditorAfterSubmit = close;
            return this;
        }

        public EditablePanelHandler build() {
            return new EditablePanelHandler(this);
        }
    }
}
