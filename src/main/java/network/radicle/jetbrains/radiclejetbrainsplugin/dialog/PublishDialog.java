package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInit;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPush;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class PublishDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField branchField;
    private JTextField nameField;
    private JTextArea descriptionField;
    private JComboBox<String> seedNodeSelect;
    private JComboBox<GitRepository> projectSelect;
    private JLabel projectNameLabel;
    private JLabel branchLabel;
    private JLabel nameLabel;
    private JLabel descrLabel;
    private JLabel seedNodeLabel;
    private final List<GitRepository> repos;
    private final Project project;
    private final RadicleSettingsHandler radicleSettingsHandler;
    private boolean isSelectedRepoInitialized;
    public CountDownLatch isUiLoaded =  new CountDownLatch(1);

    public PublishDialog(List<GitRepository> repos, Project project) {
        super(true);
        this.repos = repos;
        this.project = project;
        this.radicleSettingsHandler = new RadicleSettingsHandler();
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
            ProcessOutput output = null;
            if (!isSelectedRepoInitialized) {
                var init = new RadInit(repo, nameField.getText(), descriptionField.getText(), branchField.getText());
                output = new BasicAction(init, project, new CountDownLatch(1)).perform();
            }
            if (isSelectedRepoInitialized || output.getExitCode() == 0) {
                var countDown = new CountDownLatch(1);
                var executingFlag = new AtomicBoolean(false);
                UpdateBackgroundTask ubt = new UpdateBackgroundTask(project, RadicleBundle.message("publishProgressBar"),
                        countDown, executingFlag);
                ApplicationManager.getApplication().executeOnPooledThread(ubt::queue);
                var push = new RadPush(repo,(String) seedNodeSelect.getSelectedItem());
                new BasicAction(push,project,countDown).perform();
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
        var settings = radicleSettingsHandler.loadSettings();
        var seedNodes = settings.getSeedNodes();
        var textFieldListener = new TextFieldListener();
        nameField.getDocument().addDocumentListener(textFieldListener);
        branchField.getDocument().addDocumentListener(textFieldListener);
        branchField.setText("master");
        descriptionField.setLineWrap(true);
        descriptionField.getDocument().addDocumentListener(textFieldListener);
        projectSelect.addItemListener(e -> updateLayout());
        projectSelect.setRenderer(new ComboBoxCellRenderer());
        for (var node : seedNodes) {
            seedNodeSelect.addItem(node.host);
        }
        for (var repo : repos) {
            projectSelect.addItem(repo);
        }

        if (repos.size() == 1) {
            projectSelect.setVisible(false);
            projectNameLabel.setVisible(false);
        }
        updateLayout();
    }

    private void updateLayout() {
        var repo = (GitRepository) projectSelect.getSelectedItem();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var isInitialized = BasicAction.isProjectRadInitialized(repo);
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

                if (isInitialized || (!nameField.getText().isEmpty() && !branchField.getText().isEmpty() &&
                        !descriptionField.getText().isEmpty() && seedNodeSelect.getSelectedItem() != null)){
                    setOKActionEnabled(true);
                } else {
                    setOKActionEnabled(false);
                }
            }, ModalityState.any());
        });
    }

    private static class ComboBoxCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof GitRepository) {
                var repo = (GitRepository) value;
                setText(repo.getRoot().getName());
            }
            return this;
        }
    }

    public class TextFieldListener extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            if (nameField.getText().isEmpty() || branchField.getText().isEmpty() ||
                    descriptionField.getText().isEmpty() || seedNodeSelect.getSelectedItem() == null) {
                setOKActionEnabled(false);
            } else {
                setOKActionEnabled(true);
            }
        }
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

    public JComboBox<String> getSeedNodeSelect() {
        return seedNodeSelect;
    }

    public JLabel getSeedNodeLabel() {return seedNodeLabel;}

    public JComboBox<GitRepository> getProjectSelect() {
        return projectSelect;
    }

    public JLabel getProjectNameLabel() {
        return projectNameLabel;
    }


}