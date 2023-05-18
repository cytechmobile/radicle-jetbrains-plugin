package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;
import java.awt.Component;

public class TrackDialog extends DialogWrapper {
    private JPanel contentPane;
    private JComboBox<RadTrack.RadTrackType> trackActionSelect;
    private JLabel track;
    private JLabel peerIdLabel;
    private JTextField peerIdField;
    private JLabel alliasLabel;
    private JTextField aliasField;
    private JLabel repositoryIdLabel;
    private JTextField repositoryField;
    private JLabel scopeLabel;
    private JComboBox<RadTrack.Scope> scopeSelect;
    private final Project project;

    public TrackDialog(Project project) {
        super(true);
        this.project = project;
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            RadTrack radTrack;
            var selectedAction = (RadTrack.RadTrackType) trackActionSelect.getSelectedItem();
            if (selectedAction == RadTrack.RadTrackType.TRACK_PEER) {
                radTrack = new RadTrack(project, new RadTrack.Peer(peerIdField.getText(), aliasField.getText()));
            } else {
                radTrack = new RadTrack(project, new RadTrack.Repo(repositoryField.getText(), (RadTrack.Scope)
                        scopeSelect.getSelectedItem()));
            }
            radTrack.perform();
        });
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    protected void init() {
        super.init();
        setTitle(RadicleBundle.message("track"));
        var renderer = new ComboBoxCellRenderer();
        trackActionSelect.setRenderer(renderer);
        trackActionSelect.addItem(RadTrack.RadTrackType.TRACK_PEER);
        trackActionSelect.addItem(RadTrack.RadTrackType.TRACK_REPOSITORY);
        trackActionSelect.addActionListener(e -> {
            enableDisableButton();
            var selectedAction = (RadTrack.RadTrackType) trackActionSelect.getSelectedItem();
            if (selectedAction == RadTrack.RadTrackType.TRACK_PEER) {
                showPeerFields(true);
                showProjectFields(false);
            } else {
                showProjectFields(true);
                showPeerFields(false);
            }
        });
        scopeSelect.setRenderer(renderer);
        scopeSelect.addItem(RadTrack.Scope.NONE);
        scopeSelect.addItem(RadTrack.Scope.TRUSTED);
        scopeSelect.addItem(RadTrack.Scope.ALL);

        var fieldListner = new TextFieldListener();
        peerIdField.getDocument().addDocumentListener(fieldListner);
        repositoryField.getDocument().addDocumentListener(fieldListner);
        setOKActionEnabled(false);
        showProjectFields(false);
    }

    private void showPeerFields(boolean show) {
        peerIdLabel.setVisible(show);
        peerIdField.setVisible(show);
        aliasField.setVisible(show);
        alliasLabel.setVisible(show);
    }

    private void showProjectFields(boolean show) {
        repositoryIdLabel.setVisible(show);
        repositoryField.setVisible(show);
        scopeLabel.setVisible(show);
        scopeSelect.setVisible(show);
    }

    private static class ComboBoxCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof RadTrack.RadTrackType) {
                setText(((RadTrack.RadTrackType) value).name);
            }
            if (value instanceof RadTrack.Scope) {
                setText(((RadTrack.Scope) value).name);
            }
            return this;
        }
    }

    public JLabel getPeerIdLabel() {
        return peerIdLabel;
    }

    public JTextField getPeerIdField() {
        return peerIdField;
    }

    public JLabel getAlliasLabel() {
        return alliasLabel;
    }

    public JTextField getAliasField() {
        return aliasField;
    }

    public JLabel getRepositoryIdLabel() {
        return repositoryIdLabel;
    }

    public JTextField getRepositoryField() {
        return repositoryField;
    }

    public JLabel getScopeLabel() {
        return scopeLabel;
    }

    public JComboBox<RadTrack.Scope> getScopeSelect() {
        return scopeSelect;
    }

    public JComboBox<RadTrack.RadTrackType> getTrackActionSelect() {
        return trackActionSelect;
    }

    public class TextFieldListener extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            enableDisableButton();
        }
    }

    private void enableDisableButton() {
        var selectedTrackType = ((RadTrack.RadTrackType) trackActionSelect.getSelectedItem()).name;
        if (selectedTrackType.equals(RadTrack.RadTrackType.TRACK_PEER.name)) {
            setOKActionEnabled(!peerIdField.getText().isEmpty());
        } else {
            setOKActionEnabled(!repositoryField.getText().isEmpty());
        }
    }
}
