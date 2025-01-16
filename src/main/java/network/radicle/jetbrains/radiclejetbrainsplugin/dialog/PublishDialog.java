package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.google.common.base.Strings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ClientProperty;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextArea;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInit;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow.RAD_REPOS_KEY;

public class PublishDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField branchField;
    private JTextField nameField;
    private JBTextArea descriptionField;
    private JComboBox<GitRepository> projectSelect;
    private JLabel projectNameLabel;
    private JLabel branchLabel;
    private JLabel nameLabel;
    private JLabel descrLabel;
    private JComboBox<Visibility> visibilitySelect;
    private JLabel visibilityLabel;
    private final List<GitRepository> repos;
    private final Project project;
    private boolean isSelectedRepoInitialized;
    public CountDownLatch isUiLoaded = new CountDownLatch(1);

    public PublishDialog(List<GitRepository> repos, Project project) {
        super(true);
        this.repos = repos;
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
            var repo = (GitRepository) projectSelect.getSelectedItem();
            if (!isSelectedRepoInitialized && repo != null) {
                var visibility = ((Visibility) visibilitySelect.getSelectedItem()).value;
                var init = new RadInit(repo, nameField.getText(), descriptionField.getText(), branchField.getText(), visibility);
                var output = init.perform();
                if (RadAction.isSuccess(output)) {
                    // Show radicle tool window
                    var radToolWindow = ToolWindowManager.getInstance(repo.getProject()).getToolWindow(RadicleToolWindow.id);
                    if (radToolWindow == null || radToolWindow.isAvailable()) {
                        return;
                    }
                    repo.update();
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ClientProperty.put(radToolWindow.getComponent(), RAD_REPOS_KEY, List.of(repo));
                        radToolWindow.setAvailable(true);
                    }, ModalityState.any());
                }
            }
        });
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    protected void init() {
        super.init();
        setOKActionEnabled(false);
        setTitle(RadicleBundle.message("shareProject"));
        var textFieldListener = new TextFieldListener();
        nameField.getDocument().addDocumentListener(textFieldListener);
        branchField.getDocument().addDocumentListener(textFieldListener);
        branchField.setText("master");
        descriptionField.setBorder(branchField.getBorder());
        descriptionField.setLineWrap(true);
        descriptionField.getDocument().addDocumentListener(textFieldListener);
        projectSelect.addItemListener(e -> updateLayout());
        var comboBoxRenderer = new ComboBoxCellRenderer();
        projectSelect.setRenderer(comboBoxRenderer);
        visibilitySelect.setRenderer(comboBoxRenderer);
        for (var vi : Visibility.values()) {
            visibilitySelect.addItem(vi);
        }
        //seedNodeSelect.addItem(seedNode.url);
        for (var repo : repos) {
            projectSelect.addItem(repo);
        }
        updateLayout();
    }

    private void updateLayout() {
        var repo = (GitRepository) projectSelect.getSelectedItem();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var isInitialized = RadAction.isProjectRadInitialized(repo);
            isUiLoaded.countDown();
            isSelectedRepoInitialized = isInitialized;
            ApplicationManager.getApplication().invokeLater(() -> {
                var showInitFields = !isInitialized;
                branchField.setVisible(showInitFields);
                nameField.setVisible(showInitFields);
                descriptionField.setVisible(showInitFields);
                branchLabel.setVisible(showInitFields);
                descrLabel.setVisible(showInitFields);
                nameLabel.setVisible(showInitFields);
                visibilityLabel.setVisible(showInitFields);
                visibilitySelect.setVisible(showInitFields);

                var ok = isInitialized || (!nameField.getText().isEmpty() && !branchField.getText().isEmpty() && !descriptionField.getText().isEmpty());
                setOKActionEnabled(ok);
            }, ModalityState.any());
        });
    }

    public JTextField getBranchField() {
        return branchField;
    }

    public JTextField getNameField() {
        return nameField;
    }

    public JTextArea getDescriptionField() {
        return descriptionField;
    }

    public JComboBox<Visibility> getVisibilitySelect() {
        return visibilitySelect;
    }

    public JComboBox<GitRepository> getProjectSelect() {
        return projectSelect;
    }

    public JLabel getProjectNameLabel() {
        return projectNameLabel;
    }

    public class TextFieldListener extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            var err = Strings.isNullOrEmpty(nameField.getText()) || Strings.isNullOrEmpty(branchField.getText()) ||
                      Strings.isNullOrEmpty(descriptionField.getText());
            setOKActionEnabled(!err);
        }
    }

    public static class ComboBoxCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof GitRepository repo) {
                setText(repo.getRoot().getName());
            }
            if (value instanceof Visibility val) {
                setText(val.label);
            }
            return this;
        }
    }

    public enum Visibility {
        PUBLIC("--public", "Public"),
        PRIVATE("--private", "Private");

        public final String value;
        public final String label;

        Visibility(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }
}
