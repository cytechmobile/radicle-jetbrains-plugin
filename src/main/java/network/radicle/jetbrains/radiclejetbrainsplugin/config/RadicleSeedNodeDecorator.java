package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.AddSeedNodeDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.ConfirmationDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class RadicleSeedNodeDecorator {

    private final RadicleGlobalSettingsHandler radicleGlobalSettingsHandler;
    private RadicleGlobalSettings settings;
    protected JBTable table;
    private List<SeedNode> loadedSeedNodes;
    private List<SeedNode> cpLoadedSeedNodes;

    public RadicleSeedNodeDecorator() {
        this.radicleGlobalSettingsHandler = new RadicleGlobalSettingsHandler();
        loadSeedNodes();
    }

    public void loadSeedNodes() {
        this.settings = this.radicleGlobalSettingsHandler.loadSettings();
        this.loadedSeedNodes = this.settings.getSeedNodes();
        cpLoadedSeedNodes = new ArrayList<>(this.loadedSeedNodes);
    }

    public void initializeData() {
        removeRows();
        var tableModel = (DefaultTableModel) table.getModel();
        for (var seedNode : loadedSeedNodes) {
            tableModel.addRow(new Object[]{seedNode.host, seedNode.port});
        }
    }

    private void removeRows() {
        var tableModel = (DefaultTableModel) table.getModel();
        if (tableModel.getRowCount() > 0) {
            for (int i = tableModel.getRowCount() - 1; i > -1; i--) {
                tableModel.removeRow(i);
            }
        }
    }

    public List<SeedNode> getLoadedSeedNodes() {
        return loadedSeedNodes;
    }

    public List<SeedNode> getCpLoadedSeedNodes() {
        return cpLoadedSeedNodes;
    }

    public JTable getTable() {
        return table;
    }

    public ToolbarDecorator initDecorator() {
        var tableModel = new DefaultTableModel();
        table = new JBTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowSelectionAllowed(true);
        table.setDefaultEditor(Object.class, null);
        table.setRowHeight(20);
        tableModel.addColumn(RadicleBundle.message("seedNode"));
        tableModel.addColumn(RadicleBundle.message("port"));
        initializeData();

        var toolbarDecorator = ToolbarDecorator.createDecorator(table)
                .disableUpDownActions()
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .setAddAction(new AddSeedNode())
                .setRemoveAction(new RemoveSeedNode())
                .setEditAction(new EditSeedNode());
        return toolbarDecorator;
    }

    public class AddSeedNode implements AnActionButtonRunnable {

        public void addNode(String seedNode, String port) {
            cpLoadedSeedNodes.add(new SeedNode(seedNode, port));
            var tableModel = (DefaultTableModel) table.getModel();
            tableModel.addRow(new Object[]{seedNode, port});
        }

        @Override
        public void run(AnActionButton anActionButton) {
            var addSeedNodeDialog = new AddSeedNodeDialog(cpLoadedSeedNodes);
            var okButton = addSeedNodeDialog.showAndGet();
            if (okButton) {
                var seedNode = addSeedNodeDialog.getNodeField().getText();
                var port = addSeedNodeDialog.getPortField().getText();
                addNode(seedNode, port);
            }
        }
    }

    public class RemoveSeedNode implements AnActionButtonRunnable {

        public void removeNode(String seedNode, String port, int selectedRow) {
            var node = new SeedNode(seedNode, port);
            cpLoadedSeedNodes.removeIf(n -> n.toString().equals(node.toString()));
            var tableModel = (DefaultTableModel) table.getModel();
            tableModel.removeRow(selectedRow);
        }

        @Override
        public void run(AnActionButton anActionButton) {
            var confirmationDialog = new ConfirmationDialog(RadicleBundle.message("removeSeedNodeConfirm"),
                    RadicleBundle.message("removeSeedNode"));
            var okButton = confirmationDialog.showAndGet();
            if (okButton) {
                var selectedRow = table.getSelectedRow();
                var tableModel = (DefaultTableModel) table.getModel();
                var seedNode = (String) tableModel.getValueAt(selectedRow, 0);
                var port = (String) tableModel.getValueAt(selectedRow, 1);
                removeNode(seedNode, port, selectedRow);
            }
        }
    }

    public class EditSeedNode implements AnActionButtonRunnable {

        public void editNode(String newSeedNode, String newPort, String oldSeedNode, String oldPort, int selectedRow) {
            var tableModel = (DefaultTableModel) table.getModel();
            tableModel.setValueAt(newSeedNode, selectedRow, 0);
            tableModel.setValueAt(newPort, selectedRow, 1);
            var index = cpLoadedSeedNodes.indexOf(new SeedNode(oldSeedNode, oldPort));
            if (index > -1) {
                cpLoadedSeedNodes.set(index, new SeedNode(newSeedNode, newPort));
            }
        }

        @Override
        public void run(AnActionButton anActionButton) {
            var selectedRow = table.getSelectedRow();
            var tableModel = (DefaultTableModel) table.getModel();
            var oldSeedNode = (String) tableModel.getValueAt(selectedRow, 0);
            var oldPort = (String) tableModel.getValueAt(selectedRow, 1);
            var addSeedNodeDialog = new AddSeedNodeDialog(oldSeedNode, oldPort, cpLoadedSeedNodes);
            var okButton = addSeedNodeDialog.showAndGet();
            if (okButton) {
                var newSeedNode = addSeedNodeDialog.getNodeField().getText();
                var newPort = addSeedNodeDialog.getPortField().getText();
                editNode(newSeedNode, newPort, oldSeedNode, oldPort, selectedRow);
            }
        }
    }
}
