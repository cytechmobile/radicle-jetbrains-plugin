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
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
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
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CloneRadDialog extends VcsCloneDialogExtensionComponent implements CloneProject {
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
    protected JBLabel infoLabel;

    private final String RAD_UI_URL = "https://app.radicle.xyz/seeds/";
    private final ProjectApi projectApi;
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
        if (!BasicAction.isCliPathConfigured(project)) {
            return ;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radSelf = new RadSelf(RadSelf.RadSelfAction.ACTIVE_PROFILE);
            var output = new BasicAction(radSelf, null, new CountDownLatch(1)).perform();
            var activeProfile = output.getExitCode() == 0 ? output.getStdout().replace("\n","") : "";
            ApplicationManager.getApplication().invokeLater(() ->
                    activeProfileLabel.setText(RadicleBundle.message("activeIdentity") + activeProfile), ModalityState.any());
        });
    }

    @Override
    public String projectName(List<String> outputLines) {
        var selectedProject = radProjectJBList.getSelectedValue();
        return selectedProject.name;
    }

    @Override
    public String url() {
        var selectedProject = radProjectJBList.getSelectedValue();
        return selectedProject.radUrl;
    }

    @Override
    public String directory() {
        return directoryField.getText();
    }

    @Override
    public void doClone(@NotNull CheckoutProvider.Listener listener) {
        if (!BasicAction.isCliPathConfigured(project)) {
            return ;
        }
        CloneUtil.doClone(listener,project,this);
    }

    @NotNull
    @Override
    public List<ValidationInfo> doValidateAll() {
        var list = new ArrayList<ValidationInfo>();
        ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkDirectory(directoryField.getText(),
                directoryField.getTextField()));
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
        activeProfileLabel.setForeground(JBColor.BLACK);
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
        radProjectJBList.addListSelectionListener(new TableSelectionListener(SelectionType.PROJECT));
        radProjectJBList.addMouseListener(new ListButtonListener());
        var scrollPanel = new JPanel(new BorderLayout());
        scrollPanel.add(searchSpinner,BorderLayout.SOUTH);
        scrollPanel.add(radProjectJBList,BorderLayout.CENTER);
        projectListPanel = new JBScrollPane(scrollPanel);
        projectListPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        projectPanel = new JPanel();
        projectPanel.setLayout(new BorderLayout());
        projectPanel.setBorder(JBUI.Borders.empty(5, 5,0,0));

        var panel = new JPanel();
        panel.setLayout(new GridLayout(3,1));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(new JBLabel(RadicleBundle.message("selectProject")));
        panel.add(searchField);
        var openInBrowserLabel = new JBLabel(RadicleBundle.message("openInBrowser"));
        openInBrowserLabel.setForeground(JBColor.RED);
        panel.add(openInBrowserLabel);

        projectPanel.add(panel,BorderLayout.NORTH);
        projectPanel.add(loadMore,BorderLayout.SOUTH);
        projectPanel.add(projectListPanel,BorderLayout.CENTER);
    }

    private void initializeSeedNodePanel() {
        myDecorator = new RadicleSeedNodeDecorator();
        var toolbarDecorator = myDecorator.initDecorator();
        toolbarDecorator.addExtraAction(new RedirectButton());
        toolbarDecorator.disableRemoveAction().disableAddAction().setEditAction(null);
        myDecorator.getTable().getSelectionModel().addListSelectionListener(new TableSelectionListener(
                SelectionType.SEEDNODE));

        seedNodePanel = new JPanel();
        seedNodePanel.setLayout(new BorderLayout());
        var selectSeedNodeLabel = new JBLabel(RadicleBundle.message("selectSeedNode"));
        selectSeedNodeLabel.setBorder(JBUI.Borders.empty(5, 0,10,0));
        seedNodePanel.add(selectSeedNodeLabel, BorderLayout.NORTH);
        seedNodePanel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER);
        infoLabel = new JBLabel(RadicleBundle.message("infoLabel"));
        infoLabel.setBorder(JBUI.Borders.empty(5, 0));
        seedNodePanel.add((infoLabel), BorderLayout.SOUTH);
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

    private class ListButtonListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                var selectedProject = radProjectJBList.getSelectedValue();
                var projectUrl = RAD_UI_URL + selectedSeedNode.host + "/" + selectedProject.urn;
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(projectUrl));
                    } catch (IOException | URISyntaxException ex) {
                        logger.warn("Unable to open rad url");
                    }
                }
            }
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

    private static class CellRendered extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            var radProject = (RadProject) value;
            return  new JLabel("<html><b>Name: </b>" + radProject.name + "<br/>" +
                     "<b>Urn: </b>" + radProject.urn + "<br/>" + "<br/>" + "</html>");
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
            searchSpinner.setVisible(true);
            projectModel.clear();
            for (var pr : loadedProjects) {
                if (pr.name.toLowerCase().contains(filter) || pr.urn.toLowerCase().contains(filter)) {
                    projectModel.addElement(pr);
                }
            }
            searchSpinner.setVisible(false);
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

    protected class TableSelectionListener implements ListSelectionListener {
        private final SelectionType type;

        public TableSelectionListener(SelectionType type) {
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
                infoLabel.setVisible(false);
                loadProjects();
            }
        }
    }
}
