package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.google.common.base.Strings;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsSeedNodeView;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class CloneRadDialog extends VcsCloneDialogExtensionComponent implements CloneProject {
    private static final Logger logger = LoggerFactory.getLogger(CloneRadDialog.class);
    protected TextFieldWithBrowseButton directoryField;
    protected JPanel mainPanel;
    protected JPanel identityPanel;
    protected JPanel projectPanel;
    protected JPanel directoryPanel;
    protected JBLabel activeProfileLabel;
    protected JBScrollPane projectListPanel;
    protected JBList<RadProject> radProjectJBList;
    protected SearchTextField searchField;
    protected DefaultListModel<RadProject> projectModel;
    protected JButton loadMore;
    protected AsyncProcessIcon searchSpinner;
    protected ComboBox<SeedNode> seedNodeComboBox;
    private final RadicleSettingsHandler radicleSettingsHandler;
    private RadicleSettings settings;
    private static final String RAD_UI_URL = "https://app.radicle.xyz/seeds/";
    private final ProjectApi projectApi;
    private SeedNode selectedSeedNode;
    private final List<RadProject> loadedProjects;
    private final Project project;
    protected int page;
    private boolean triggerSeedNodeAction = true;

    public enum TextFieldType {
        SEARCH_FIELD, BROWSE_FIELD
    }

    public CloneRadDialog(@NotNull Project project, ProjectApi api) {
        this.loadedProjects = new ArrayList<>();
        this.project = project;
        this.projectApi = api;
        this.radicleSettingsHandler = new RadicleSettingsHandler();
        this.settings = this.radicleSettingsHandler.loadSettings();
        initializeIdentityPanel();
        initializeProjectPanel();
        initializeMainPanel();
    }

    public CloneRadDialog(@NotNull Project project) {
        this(project, new ProjectApi());
    }

    private void setActiveProfile() {
        if (!RadAction.isCliPathConfigured(project)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radSelf = new RadSelf();
            var output = radSelf.perform();
            String activeNodeId;
            if (RadAction.isSuccess(output)) {
                var radDetails = new RadDetails(output.getStdoutLines(true));
                activeNodeId = radDetails.nodeId;
            } else {
                activeNodeId = "";
            }
            ApplicationManager.getApplication().invokeLater(() ->
                    activeProfileLabel.setText(RadicleBundle.message("activeIdentity") + activeNodeId), ModalityState.any());
        });
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
        if (!RadAction.isCliPathConfigured(project)) {
            return;
        }
        CloneUtil.doClone(listener, project, this);
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
        directoryPanel.add(new JLabel(RadicleBundle.message("directory")), BorderLayout.WEST);
        directoryPanel.add(directoryField, BorderLayout.CENTER);
        directoryField.getTextField().getDocument().addDocumentListener(new TextFieldListener(TextFieldType.BROWSE_FIELD));
        directoryField.addBrowseFolderListener(RadicleBundle.message("selectDirectory"), "", null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor());
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(4));
        mainPanel.add(identityPanel, BorderLayout.NORTH);
        mainPanel.add(projectPanel, BorderLayout.CENTER);
        mainPanel.add(directoryPanel, BorderLayout.SOUTH);
    }

    private void initializeIdentityPanel() {
        activeProfileLabel = new JBLabel("");
        activeProfileLabel.setFont(new Font(activeProfileLabel.getFont().getName(),
                Font.BOLD, activeProfileLabel.getFont().getSize()));
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
        radProjectJBList.addListSelectionListener(new TableSelectionListener());
        radProjectJBList.addMouseListener(new ListButtonListener());
        var scrollPanel = new JPanel(new BorderLayout());
        scrollPanel.add(searchSpinner, BorderLayout.SOUTH);
        scrollPanel.add(radProjectJBList, BorderLayout.CENTER);
        projectListPanel = new JBScrollPane(scrollPanel);
        projectListPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        projectPanel = new JPanel();
        projectPanel.setLayout(new BorderLayout());
        projectPanel.setBorder(JBUI.Borders.empty(5, 5, 0, 0));

        var gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(4, 1));
        gridPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        var panel = new JPanel(new BorderLayout());
        panel.add(new JBLabel(RadicleBundle.message("selectSeedNode")), BorderLayout.WEST);
        Presentation presentation = new Presentation();
        presentation.setIcon(AllIcons.General.Settings);
        panel.add(new ActionButton(new SeedNodeViewAction(), presentation, ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE), BorderLayout.EAST);
        gridPanel.add(panel);
        seedNodeComboBox = new ComboBox<>();
        initializeSeedNodeCombobox();
        seedNodeComboBox.setSelectedIndex(-1);
        seedNodeComboBox.addActionListener(new SeedNodeListener());
        seedNodeComboBox.setRenderer(new SeedNodeCellRenderer());
        gridPanel.add(seedNodeComboBox);
        gridPanel.add(new JBLabel(RadicleBundle.message("selectProject")));
        gridPanel.add(searchField);
        projectPanel.add(gridPanel, BorderLayout.NORTH);

        var bottomGrid = new JPanel(new GridLayout(2, 1));
        bottomGrid.add(loadMore);
        bottomGrid.add(new JBLabel(RadicleBundle.message("openInBrowser")));
        projectPanel.add(bottomGrid, BorderLayout.SOUTH);
        projectPanel.add(projectListPanel, BorderLayout.CENTER);
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
                RadAction.showErrorNotification(project, RadicleBundle.message("httpRequestErrorTitle"),
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

    private class SeedNodeListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (seedNodeComboBox.getSelectedItem() != null && triggerSeedNodeAction) {
                loadProjects();
            }
        }
    }

    private class ListButtonListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            var selectedProject = radProjectJBList.getSelectedValue();
            if (e.getClickCount() == 2 && selectedSeedNode != null && selectedProject != null) {
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

    public static class SeedNodeCellRenderer implements ListCellRenderer<SeedNode> {
        @Override
        public Component getListCellRendererComponent(JList<? extends SeedNode> list, SeedNode node, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (node == null) {
                return new JLabel("");
            }
            return new JLabel(node.host);
        }
    }

    private void initializeSeedNodeCombobox() {
        settings = radicleSettingsHandler.loadSettings();
        var loadedSeedNodes = settings.getSeedNodes();
        seedNodeComboBox.removeAllItems();
        for (var node : loadedSeedNodes) {
            seedNodeComboBox.addItem(node);
        }
    }

    private class SeedNodeViewAction extends AnAction {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            triggerSeedNodeAction = false;
            ShowSettingsUtil.getInstance().showSettingsDialog(project, RadicleSettingsSeedNodeView.class);
            var prevSelectedIndex = seedNodeComboBox.getSelectedIndex();
            initializeSeedNodeCombobox();
            seedNodeComboBox.setSelectedIndex(prevSelectedIndex);
            triggerSeedNodeAction = true;
        }
    }

    private static class CellRendered extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            var radProject = (RadProject) value;
            return new JLabel("<html><b>Name: </b>" + radProject.name + "<br/>" +
                    "<b>Urn: </b>" + radProject.urn + "<br/>" + "<br/>" + "</html>");
        }
    }

    private class TextFieldListener extends DocumentAdapter {
        private final TextFieldType type;

        public TextFieldListener(TextFieldType tft) {
            this.type = tft;
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

    protected void loadProjects() {
        page = 0;
        projectModel.clear();
        selectedSeedNode = (SeedNode) seedNodeComboBox.getSelectedItem();
        fetchProjects();
    }

    protected class TableSelectionListener implements ListSelectionListener {

        private void enableCloneButton() {
            if (!Strings.isNullOrEmpty(directoryField.getText())) {
                CloneRadDialog.super.getDialogStateListener().onOkActionEnabled(true);
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                enableCloneButton();
            }
        }
    }
}
