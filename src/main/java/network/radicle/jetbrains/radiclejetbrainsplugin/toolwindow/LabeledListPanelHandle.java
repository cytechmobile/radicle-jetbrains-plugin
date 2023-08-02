package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.WrapLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract  class LabeledListPanelHandle<T> {
    private final InlineIconButton editButton;
    private final JLabel progressLabel;
    private final JLabel errorIcon;
    private final JLabel titleLabel;
    public JBPopup jbPopup;
    public JBPopupListener listener;

    public LabeledListPanelHandle() {
        progressLabel = new JLabel(new AnimatedIcon.Default());
        progressLabel.setBorder(JBUI.Borders.empty(6, 0));
        errorIcon = new JLabel(AllIcons.General.Error);
        errorIcon.setBorder(JBUI.Borders.empty(6, 0));
        progressLabel.setVisible(false);
        errorIcon.setVisible(false);
        titleLabel = new JLabel();
        titleLabel.setForeground(UIUtil.getContextHelpForeground());
        titleLabel.setBorder(JBUI.Borders.empty(6, 5));
        titleLabel.setText(getLabel());
        editButton = new InlineIconButton(AllIcons.General.Inline_edit, AllIcons.General.Inline_edit_hovered);
        editButton.setBorder(JBUI.Borders.empty(6, 0));
        editButton.setActionListener(e ->
                showEditPopup(editButton).thenComposeAsync(data -> {
                    progressLabel.setVisible(true);
                    errorIcon.setVisible(false);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var isSuccess = storeValues(data);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            progressLabel.setVisible(false);
                            if (!isSuccess) {
                                errorIcon.setVisible(true);
                            }
                        }, ModalityState.any());
                    });
                    return null;
                }));
    }

    private JPanel getControlsPanel() {
        var controlsPanel = new JPanel(new HorizontalLayout(4));
        controlsPanel.setOpaque(false);
        controlsPanel.add(editButton);
        controlsPanel.add(progressLabel);
        controlsPanel.add(errorIcon);
        return controlsPanel;
    }

    public JLabel getTitleLabel() {
        return titleLabel;
    }

    public JComponent getPanel() {
        var simplePanel = JBUI.Panels.simplePanel();
        simplePanel.setOpaque(false);
        simplePanel.add(new JLabel(getSelectedValues()));
        simplePanel.addToRight(getControlsPanel());
        var panel = new NonOpaquePanel(new WrapLayout(FlowLayout.LEADING, 0, 0));
        panel.add(simplePanel);
        return panel;
    }
    public CompletableFuture<List<T>> showEditPopup(JComponent parent) {
        var result = new CompletableFuture<List<T>>();
        var popUpBuilder = new PopupBuilder();
        jbPopup = popUpBuilder.createPopup(this.getData(), getRender(), this.isSingleSelection(), null, result);
        jbPopup.showUnderneathOf(parent);
        listener = popUpBuilder.getListener();
        return result;
    }

    public abstract String getSelectedValues();

    public abstract boolean storeValues(java.util.List<T> data);

    public abstract SelectionListCellRenderer<T> getRender();

    public abstract CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<T>>> getData();

    public abstract String getLabel();

    public abstract boolean isSingleSelection();
}
