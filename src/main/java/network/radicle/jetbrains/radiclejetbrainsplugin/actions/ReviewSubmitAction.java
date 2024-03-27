package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.newui.InstallButton;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actions.IncrementalFindAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.ui.ComponentContainer;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.serviceContainer.NonInjectable;
import com.intellij.ui.EditorSettingsProvider;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.JButtonAction;
import com.intellij.util.ui.UIUtil;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchComponentFactory;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ReviewSubmitAction extends JButtonAction {
    private RadPatch patch;
    private JBPopup popup;
    private EditorTextField editor;
    private JButton approveButton;
    private JButton rejectButton;
    private InlineIconButton closeButton;
    private ComponentContainer container;

    public ReviewSubmitAction() {
        super(RadicleBundle.message("submit"), "", null);
    }

    @NonInjectable
    public ReviewSubmitAction(RadPatch patch) {
        super(RadicleBundle.message("submit"), "", null);
        this.patch = patch;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var radPatch = e.getProject().getUserData(PatchComponentFactory.PATCH_DIFF);
        if (radPatch == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        patch = radPatch;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var parentComponent = e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY);
        if (parentComponent == null) {
            return;
        }
        setUpPopup(parentComponent);
    }

    public void setUpPopup(JComponent parentComponent) {
        var cancelActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popup.cancel();
            }
        };
        container = createPopupComponent(cancelActionListener);
        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(container.getComponent(), container.getPreferredFocusableComponent())
                .setFocusable(true)
                .setRequestFocus(true)
                .setResizable(true)
                .createPopup();
        popup.showUnderneathOf(parentComponent);
    }

    private void submitReview(RadPatch.Review.Verdict verdict, String summary) {
        disableUI();
        Runnable submitTask = () -> {
            var api = patch.project.getService(RadicleProjectApi.class);
            var res = api.addReview(verdict.getValue(), summary, patch);
            var success = res != null;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!success) {
                    enableUI();
                } else {
                    popup.cancel();
                }
            });
        };
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            submitTask.run();
        } else {
            ApplicationManager.getApplication().executeOnPooledThread(submitTask);
        }
    }

    private void disableUI() {
        editor.setEnabled(false);
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        closeButton.setEnabled(false);
    }

    private void enableUI() {
        editor.setEnabled(true);
        approveButton.setEnabled(true);
        rejectButton.setEnabled(true);
        closeButton.setEnabled(true);
    }

    private ComponentContainer createPopupComponent(ActionListener cancelActionListener) {
        var wrapper = new Wrapper();
        wrapper.setOpaque(true);
        wrapper.setBackground(JBUI.CurrentTheme.List.BACKGROUND);
        wrapper.setPreferredSize(new JBDimension(500, 200));
        editor = createEditor();
        approveButton = new InstallButton(RadicleBundle.message("approve"), true) {
            @Override
            protected void setTextAndSize() { }
        };
        approveButton.addActionListener(e -> submitReview(RadPatch.Review.Verdict.ACCEPT, editor.getText()));

        rejectButton = new JButton(RadicleBundle.message("requestChanges"));
        rejectButton.addActionListener(e -> submitReview(RadPatch.Review.Verdict.REJECT, editor.getText()));
        rejectButton.setOpaque(true);

        closeButton = new InlineIconButton(AllIcons.Actions.Close, AllIcons.Actions.CloseHovered);
        closeButton.setBorder(JBUI.Borders.empty(5));
        closeButton.setActionListener(cancelActionListener);

        var titleLabel = new JLabel(RadicleBundle.message("submitReview"));
        var titlePAnel = new JPanel(new HorizontalLayout(0));
        titlePAnel.setOpaque(false);
        titlePAnel.add(titleLabel, HorizontalLayout.LEFT);
        titlePAnel.add(closeButton, HorizontalLayout.RIGHT);

        var buttonsPanel = new ScrollablePanel();
        buttonsPanel.add(approveButton);
        buttonsPanel.add(rejectButton);
        var panel = new JPanel(new MigLayout(new LC().insets("12").fill().flowY().noGrid().hideMode(3)));
        panel.setOpaque(false);
        panel.add(titlePAnel, new CC().growX());
        panel.add(editor, new CC().growX().growY());
        panel.add(buttonsPanel, new CC());

        wrapper.setContent(panel);
        wrapper.repaint();
        return new ComponentContainer() {
            @Override
            public @NotNull JComponent getComponent() {
                return wrapper;
            }

            @Override
            public JComponent getPreferredFocusableComponent() {
                return editor;
            }

            @Override
            public void dispose() { }
        };
    }

    private EditorTextField createEditor() {
        editor = new EditorTextField();
        editor.setOneLineMode(false);
        editor.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
        editor.setPlaceholder(RadicleBundle.message("review"));
        var settingsProvider = new EditorSettingsProvider() {
            @Override
            public void customizeSettings(EditorEx myEditor) {
                myEditor.getSettings().setUseSoftWraps(true);
                myEditor.setVerticalScrollbarVisible(true);
                myEditor.getScrollPane().setViewportBorder(JBUI.Borders.emptyLeft(4));
                myEditor.putUserData(IncrementalFindAction.SEARCH_DISABLED, true);
            }
        };
        editor.addSettingsProvider(settingsProvider);
        return editor;
    }

    public ComponentContainer getContainer() {
        return container;
    }
}
