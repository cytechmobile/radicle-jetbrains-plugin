package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.AddSeedNodeDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class RadicleSeedNodeDecorator {

    private final RadicleSettingsHandler radicleSettingsHandler;
    private RadicleSettings settings;
    protected JBTable table;
    private List<String> loadedSeedNodes;
    private ArrayList<String> cpLoadedSeedNodes;

    public RadicleSeedNodeDecorator() {
        this.radicleSettingsHandler = new RadicleSettingsHandler();
        this.settings = this.radicleSettingsHandler.loadSettings();
        this.loadedSeedNodes = this.settings.getSeedNodes();
        cpLoadedSeedNodes = new ArrayList<>(this.loadedSeedNodes);
    }

    private String concatenate(String seedNode, String port) {
        return seedNode + RadicleSettingsHandler.RAD_SEED_SEPERATOR + port;
    }

    private void initializeData() {
        var tableModel = (DefaultTableModel) table.getModel();
        for (var seedNode : loadedSeedNodes) {
            var parts = seedNode.split("\\" + RadicleSettingsHandler.RAD_SEED_SEPERATOR);
            var node = parts[0];
            var port = parts[1];
            tableModel.addRow(new Object[]{node,port});
        }
    }

    public List<String> getLoadedSeedNodes() {
        return loadedSeedNodes;
    }

    public ArrayList<String> getCpLoadedSeedNodes() {
        return cpLoadedSeedNodes;
    }

    public ToolbarDecorator initDecorator() {
        var tableModel = new DefaultTableModel();
        table = new JBTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowSelectionAllowed(true);
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
            cpLoadedSeedNodes.add(concatenate(seedNode,port));
            var tableModel = (DefaultTableModel) table.getModel();
            tableModel.addRow(new Object[]{seedNode,port});
        }

        @Override
        public void run(AnActionButton anActionButton) {
            var addSeedNodeDialog = new AddSeedNodeDialog(cpLoadedSeedNodes);
            var okButton = addSeedNodeDialog.showAndGet();
            if (okButton) {
                var seedNode = addSeedNodeDialog.getNodeField().getText();
                var port = addSeedNodeDialog.getPortField().getText();
                addNode(seedNode,port);
            }
        }
    }

    public class RemoveSeedNode implements AnActionButtonRunnable {

        public void removeNode(String seedNode, String port, int selectedRow) {
            cpLoadedSeedNodes.remove(concatenate(seedNode,port));
            var tableModel = (DefaultTableModel) table.getModel();
            tableModel.removeRow(selectedRow);
        }

        @Override
        public void run(AnActionButton anActionButton) {
            var selectedRow =  table.getSelectedRow();
            var tableModel = (DefaultTableModel) table.getModel();
            var seedNode = (String) tableModel.getValueAt(selectedRow,0);
            var port = (String) tableModel.getValueAt(selectedRow,1);
            removeNode(seedNode,port,selectedRow);
        }
    }

    public class EditSeedNode implements AnActionButtonRunnable {

        public void editNode(String newSeedNode, String newPort, String oldSeedNode, String oldPort, int selectedRow) {
            var tableModel = (DefaultTableModel) table.getModel();
            tableModel.setValueAt(newSeedNode,selectedRow,0);
            tableModel.setValueAt(newPort,selectedRow,1);
            var index = cpLoadedSeedNodes.indexOf(concatenate(oldSeedNode,oldPort));
            cpLoadedSeedNodes.set(index, concatenate(newSeedNode, newPort));
        }

        @Override
        public void run(AnActionButton anActionButton) {
            var selectedRow =  table.getSelectedRow();
            var tableModel = (DefaultTableModel) table.getModel();
            var oldSeedNode = (String) tableModel.getValueAt(selectedRow,0);
            var oldPort = (String) tableModel.getValueAt(selectedRow,1);
            var addSeedNodeDialog = new AddSeedNodeDialog(oldSeedNode,oldPort,cpLoadedSeedNodes);
            var okButton = addSeedNodeDialog.showAndGet();
            if (okButton) {
                var newSeedNode = addSeedNodeDialog.getNodeField().getText();
                var newPort = addSeedNodeDialog.getPortField().getText();
                editNode(newSeedNode,newPort, oldSeedNode,oldPort,selectedRow);
            }
        }
    }
}
