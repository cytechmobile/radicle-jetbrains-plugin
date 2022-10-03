package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.google.common.base.Strings;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.DumbAwareActionButton;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadClone;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSeedNodeDecorator;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsSeedNodeView;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloneRadDialog extends VcsCloneDialogExtensionComponent  {
    private static final Logger logger = LoggerFactory.getLogger(CloneRadDialog.class);

    protected TextFieldWithBrowseButton directoryField;
    protected JPanel mainPanel;
    protected JPanel identityPanel;
    protected JPanel seedNodePanel;
    protected JPanel projectPanel;
    protected JPanel directoryPanel;
    protected JBLabel activeProfileLabel;
    protected JBScrollPane projectListPanel;
    protected JBList<RadProject> radProjectJBList;
    protected SearchTextField searchField;
    protected DefaultListModel<RadProject> projectModel;
    protected RadicleSeedNodeDecorator myDecorator;
    protected JButton loadMore;
    protected AsyncProcessIcon searchSpinner;

    private ProjectApi projectApi;
    private SeedNode selectedSeedNode;
    private final List<RadProject> loadedProjects;
    private final Project project;
    protected int page;

    public enum SelectionType {
        SEEDNODE, PROJECT
    }

    public enum TextFieldType {
        SEARCH_FIELD,BROWSE_FIELD
    }

    public CloneRadDialog(@NotNull Project project,ProjectApi api) {
        this.loadedProjects = new ArrayList<>();
        this.project = project;
        this.projectApi = api;
        initializeIdentityPanel();
        initializeProjectPanel();
        initializeSeedNodePanel();
        initializeMainPanel();
    }

    public CloneRadDialog(@NotNull Project project) {
        this(project,new ProjectApi());
    }

    private void setActiveProfile() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radSelf = new RadSelf(RadSelf.RadSelfAction.ACTIVE_PROFILE);
            var output = new BasicAction(radSelf, null, new CountDownLatch(1)).perform();
            var activeProfile = output.getExitCode() == 0 ? output.getStdout().replace("\n","") : "";
            ApplicationManager.getApplication().invokeLater(() ->
                    activeProfileLabel.setText(RadicleBundle.message("activeIdentity") + activeProfile), ModalityState.any());
        });
    }

    @Override
    public void doClone(@NotNull CheckoutProvider.Listener listener) {
        var parent = Paths.get(directoryField.getText()).toAbsolutePath().getParent();
        var destinationValidation = CloneDvcsValidationUtils.createDestination(parent.toString());
        if (destinationValidation != null) {
            BasicAction.showErrorNotification(project, RadicleBundle.message("cloneFailed"),
                    RadicleBundle.message("directoryError"));
            logger.error("Clone Failed. Unable to create destination directory");
            return ;
        }
        var lfs = LocalFileSystem.getInstance();
        var destinationParent = lfs.findFileByIoFile(parent.toFile());
        if (destinationParent == null) {
            destinationParent = lfs.refreshAndFindFileByIoFile(parent.toFile());
        }
        if (destinationParent == null) {
            BasicAction.showErrorNotification(project, RadicleBundle.message("cloneFailed"),
                    RadicleBundle.message("destinationDoesntExist"));
            logger.error("Clone Failed. Destination doesn't exist");
            return ;
        }
        var directoryName = Paths.get(directoryField.getText()).getFileName().toString();
        var parentDirectory = parent.toAbsolutePath().toString();
        File directory = new File(parentDirectory, directoryName);

        var selectedProject = radProjectJBList.getSelectedValue();
        var clone = new RadClone(selectedProject.radUrl,parentDirectory);
        var countDownLatch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var pr = new BasicAction(clone,project,countDownLatch).perform();
            if (pr.getExitCode() != 0) {
                return ;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                File oldDirectory = new File(parentDirectory,selectedProject.name);
                var success = oldDirectory.renameTo(directory);
                if (success) {
                    listener.directoryCheckedOut(directory,null);
                } else {
                    listener.directoryCheckedOut(oldDirectory,null);
                }
                listener.checkoutCompleted();
            });
        });
        var ubt = new UpdateBackgroundTask(project, RadicleBundle.message("cloningProcess") +
                selectedProject.urn, countDownLatch,  new AtomicBoolean(false));
        new Thread(ubt::queue).start();
    }

    @NotNull
    @Override
    public List<ValidationInfo> doValidateAll() {
        var list = new ArrayList<ValidationInfo>();
        ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkDirectory(directoryField.getText(),
                (JComponent) directoryField.getTextField()));
        return list;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return super.getPreferredFocusedComponent();
    }

    @NotNull
    @Override
    public JComponent getView() {
        return mainPanel;
    }

    @Override
    public void onComponentSelected() {
    }

    private void initializeMainPanel() {
        directoryPanel = new JPanel(new BorderLayout());
        directoryField = new TextFieldWithBrowseButton();
        directoryPanel.add(new JLabel(RadicleBundle.message("directory")),BorderLayout.WEST);
        directoryPanel.add(directoryField,BorderLayout.CENTER);
        directoryField.getTextField().getDocument().addDocumentListener(new TextFieldListener(TextFieldType.BROWSE_FIELD));
        directoryField.addBrowseFolderListener(RadicleBundle.message("selectDirectory"), "", null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor());
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(4));
        mainPanel.add(identityPanel,BorderLayout.NORTH);
        mainPanel.add(seedNodePanel, BorderLayout.WEST);
        mainPanel.add(projectPanel,BorderLayout.CENTER);
        mainPanel.add(directoryPanel,BorderLayout.SOUTH);
    }

    private void initializeIdentityPanel() {
        activeProfileLabel = new JBLabel("");
        activeProfileLabel.setFont(new Font(activeProfileLabel.getFont().getName(),
                Font.BOLD,activeProfileLabel.getFont().getSize()));
        activeProfileLabel.setForeground(JBColor.LIGHT_GRAY);
        identityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        identityPanel.setBorder(JBUI.Borders.empty(2, 0));
        identityPanel.setBackground(JBColor.GRAY);
        identityPanel.add(activeProfileLabel);
        setActiveProfile();
    }

    private void initializeProjectPanel() {
        searchSpinner = new AsyncProcessIcon(RadicleBundle.message("loadingProjects"));
        searchSpinner.setVisible(false);
        loadMore = new JButton(RadicleBundle.message("loadMore"));
        loadMore.setEnabled(false);
        loadMore.addActionListener(new LoadButtonListener());
        searchField = new SearchTextField(false);
        searchField.addDocumentListener(new TextFieldListener(TextFieldType.SEARCH_FIELD));
        projectModel = new DefaultListModel<>();

        radProjectJBList = new JBList<>(projectModel);
        radProjectJBList.setCellRenderer(new CellRendered());
        radProjectJBList.addListSelectionListener(new ListSelectionListener(SelectionType.PROJECT));

        var scrollPanel = new JPanel(new BorderLayout());
        scrollPanel.add(searchSpinner,BorderLayout.SOUTH);
        scrollPanel.add(radProjectJBList,BorderLayout.CENTER);

        projectListPanel = new JBScrollPane(scrollPanel);
        projectListPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        projectPanel = new JPanel();
        projectPanel.setLayout(new BorderLayout());
        projectPanel.setBorder(JBUI.Borders.empty(5, 5,0,0));

        projectPanel.add(searchField,BorderLayout.NORTH);
        projectPanel.add(loadMore,BorderLayout.SOUTH);
        projectPanel.add(projectListPanel,BorderLayout.CENTER);
    }

    private void initializeSeedNodePanel() {
        myDecorator = new RadicleSeedNodeDecorator();
        var toolbarDecorator = myDecorator.initDecorator();
        toolbarDecorator.addExtraAction(new RedirectButton());
        toolbarDecorator.disableRemoveAction().disableAddAction().setEditAction(null);
        myDecorator.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener(
                SelectionType.SEEDNODE));

        seedNodePanel = new JPanel();
        seedNodePanel.setLayout(new BoxLayout(seedNodePanel,BoxLayout.Y_AXIS));
        seedNodePanel.add(toolbarDecorator.createPanel());
        seedNodePanel.setPreferredSize(new Dimension(250,Integer.MAX_VALUE));
        seedNodePanel.setBorder(JBUI.Borders.emptyTop(5));
    }

    private void fetchProjects() {
        searchSpinner.setVisible(true);
        loadMore.setEnabled(false);
        SwingUtilities.invokeLater(() -> {
            /* scroll to the bottom */
            var bar = projectListPanel.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radProjects = projectApi.fetchRadProjects(selectedSeedNode, page);
            if (radProjects == null) {
                BasicAction.showErrorNotification(project, RadicleBundle.message("httpRequestErrorTitle"),
                        RadicleBundle.message("httpRequestErrorDesc"));
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (radProjects != null) {
                    loadedProjects.addAll(radProjects);
                    radProjects.forEach(p -> projectModel.addElement(p));
                }
                if (radProjects == null || !radProjects.isEmpty()) {
                    loadMore.setEnabled(true);
                }
                searchSpinner.setVisible(false);
            }, ModalityState.any());
        });
    }


    protected class LoadButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            page++;
            fetchProjects();
        }
    }

    private class RedirectButton extends DumbAwareActionButton {
        public RedirectButton() {
            super(RadicleBundle.message("manageSeedNodes"), "", PlatformIcons.SHOW_SETTINGS_ICON);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            /* Change window and reload the settings */
            ShowSettingsUtil.getInstance().showSettingsDialog(project, RadicleSettingsSeedNodeView.class);
            myDecorator.loadSeedNodes();
            myDecorator.initializeData();
        }
    }

    private class CellRendered implements ListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            var radProject = (RadProject) value;
            return new JLabel("<html>" + radProject.urn + "<br/> " + radProject.name + "<br/>" +
                    radProject.description + "<br/></html>", SwingConstants.LEFT);
        }
    }

    private class TextFieldListener extends DocumentAdapter {
        private final TextFieldType type;

        public TextFieldListener(TextFieldType type) {
            this.type = type;
        }

        private void enableDisableCloneButton() {
            if (Strings.isNullOrEmpty(directoryField.getTextField().getText())) {
                CloneRadDialog.super.getDialogStateListener().onOkActionEnabled(false);
            }
            if (radProjectJBList.getSelectedValue() != null &&
                    !Strings.isNullOrEmpty(directoryField.getTextField().getText())) {
                CloneRadDialog.super.getDialogStateListener().onOkActionEnabled(true);
            }
        }

        private void filterProjectTable() {
            var filter = searchField.getText().toLowerCase();
            projectModel.clear();
            for (var pr : loadedProjects) {
                if (pr.description.toLowerCase().contains(filter) || pr.name.toLowerCase().contains(filter) ||
                        pr.urn.toLowerCase().contains(filter)) {
                    projectModel.addElement(pr);
                }
            }
        }

        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            if (type == TextFieldType.BROWSE_FIELD) {
                enableDisableCloneButton();
            }

            if (type == TextFieldType.SEARCH_FIELD) {
                filterProjectTable();
            }
        }
    }

    protected class ListSelectionListener implements javax.swing.event.ListSelectionListener {
        private final SelectionType type;

        public ListSelectionListener(SelectionType type) {
            this.type = type;
        }

        protected void loadProjects() {
            var selectedRow = myDecorator.getTable().getSelectedRow();
            if (selectedRow != -1) {
                page = 0;
                projectModel.clear();
                var seedName = (String) myDecorator.getTable().getModel()
                        .getValueAt(myDecorator.getTable().getSelectedRow(),0);
                var port = (String) myDecorator.getTable().getModel()
                        .getValueAt(myDecorator.getTable().getSelectedRow(),1);
                selectedSeedNode = new SeedNode(seedName,port);
                fetchProjects();
            }
        }

        private void enableCloneButton() {
            if (!Strings.isNullOrEmpty(directoryField.getText())) {
                CloneRadDialog.super.getDialogStateListener().onOkActionEnabled(true);
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting() && type == SelectionType.PROJECT) {
                enableCloneButton();
            }
            if (!e.getValueIsAdjusting() && type == SelectionType.SEEDNODE) {
                loadProjects();
            }
        }
    }
}
