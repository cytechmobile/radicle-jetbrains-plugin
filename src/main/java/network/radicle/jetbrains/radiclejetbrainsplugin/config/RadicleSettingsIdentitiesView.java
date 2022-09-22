package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.JBUI;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAuth;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.ConfirmationDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.IdentityDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class RadicleSettingsIdentitiesView implements SearchableConfigurable {
    public static final String ID = RadicleBundle.message("radicle");
    protected JPanel mainPanel;
    protected JBTable table;
    private Icon checkIcon = RadicleIcons.CheckIcon;

    @Override
    public @NotNull @NonNls String getId() {
        return ID;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return ID;
    }

    public String getActiveProfile() {
        var radSelf = new RadSelf(RadSelf.RadSelfAction.ACTIVE_PROFILE);
        var output = new BasicAction(radSelf, null, new CountDownLatch(1)).perform();
        var activeProfile = output.getExitCode() == 0 ? output.getStdout().replace("\n","") : "";
        return activeProfile;
    }

    public List<String> getProfiles() {
        var radSelf = new RadSelf(RadSelf.RadSelfAction.GET_PROFILES);
        var output = new BasicAction(radSelf,null, new CountDownLatch(1)).perform();
        var loadedProfiles = output.getExitCode() == 0 ? Arrays.asList(output.getStdout().split(",")) : List.<String>of();
        return loadedProfiles;
    }

    private void initializeData() {
        removeRows();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var activeProfile = getActiveProfile();
            var loadedProfiles = getProfiles();
            if (!loadedProfiles.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    var tableModel = (DefaultTableModel) table.getModel();
                    for (var profile : loadedProfiles) {
                        if (profile.equals(activeProfile)) {
                            tableModel.addRow(new Object[]{profile,checkIcon});
                        } else {
                            tableModel.addRow(new Object[]{profile});
                        }
                    }
                }, ModalityState.any());
            }
        });
    }

    private void removeRows() {
        var tableModel = (DefaultTableModel) table.getModel();
        if (tableModel.getRowCount() > 0) {
            for (int i = tableModel.getRowCount() - 1; i > -1; i--) {
                tableModel.removeRow(i);
            }
        }
    }

    private void initComponents() {
        initTable();
    }

    private void initTable() {
        var tableModel = new DefaultTableModel(){
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Icon.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };

        table = new JBTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowSelectionAllowed(true);
        table.setRowHeight(30);

        tableModel.addColumn(RadicleBundle.message("peerId"));
        tableModel.addColumn(RadicleBundle.message("default"));

        var toolbarDecorator = ToolbarDecorator.createDecorator(table)
                .disableUpDownActions()
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .setAddAction(new AddProfileButton())
                .setRemoveAction(new RemoveProfileButton())
                .addExtraAction(new DefaultProfileButton())
                .setRemoveActionUpdater(e -> isActiveProfileRowSelected());

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(toolbarDecorator.createPanel(),BorderLayout.CENTER);
        mainPanel.setBorder(JBUI.Borders.empty(4));

        table.getColumnModel().getColumn(0).setPreferredWidth(500);
        table.getColumnModel().getColumn(1).setPreferredWidth(10);
        initializeData();
    }

    @Override
    public @Nullable JComponent createComponent() {
        reset();
        initComponents();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() {

    }

    private boolean isActiveProfileRowSelected() {
        var tableModel = (DefaultTableModel) table.getModel();
        return table.getSelectedRow() != -1 && tableModel.getValueAt(table.getSelectedRow(),1) == null;
    }

    public class RemoveProfileButton implements AnActionButtonRunnable {

        public void removeProfile(String profile) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var auth = new RadAuth(profile,"", RadAuth.RadAuthAction.REMOVE_IDENTITY);
                new BasicAction(auth,null,new CountDownLatch(1)).perform();
                initializeData();
            });
        }

        @Override
        public void run(AnActionButton anActionButton) {
            var confirmDialog = new ConfirmationDialog(RadicleBundle.message("removeIdentityConfirm"),
                    RadicleBundle.message("removeIdentity"));
            var selectedRow =  table.getSelectedRow();
            var tableModel = (DefaultTableModel) table.getModel();
            var profile = (String) tableModel.getValueAt(selectedRow,0);
            var okButton = confirmDialog.showAndGet();
            if (okButton) {
                removeProfile(profile);
            }
        }
    }

    public class AddProfileButton implements AnActionButtonRunnable {

        public void addProfile(String name, String passphrase) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var auth = new RadAuth(name, passphrase,
                        RadAuth.RadAuthAction.CREATE_IDENTITY);
                new BasicAction(auth, null, new CountDownLatch(1)).perform();
                initializeData();
            });
        }

        @Override
        public void run(AnActionButton anActionButton) {
            var dialog = new IdentityDialog();
            var okButton = dialog.showAndGet();
            if (okButton) {
                addProfile(dialog.nameField.getText(), dialog.passphraseField.getText());
            }
        }
    }

    public class DefaultProfileButton extends AnActionButton {

        public DefaultProfileButton() {
            super(RadicleBundle.message("default"),"", PlatformIcons.CHECK_ICON);
        }

        public void setDefaultProfile(String profile) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var auth = new RadAuth(profile, "",
                        RadAuth.RadAuthAction.SET_DEFAULT_IDENTITY);
                new BasicAction(auth, null, new CountDownLatch(1)).perform();
                initializeData();
            });
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            var selectedRow =  table.getSelectedRow();
            var tableModel = (DefaultTableModel) table.getModel();
            var profile = (String) tableModel.getValueAt(selectedRow, 0);
            setDefaultProfile(profile);
        }

        @Override
        public boolean isEnabled() {
            return isActiveProfileRowSelected();
        }
    }
}
