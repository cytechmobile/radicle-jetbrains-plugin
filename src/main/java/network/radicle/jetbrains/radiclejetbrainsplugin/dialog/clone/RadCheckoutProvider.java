package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.google.common.base.Strings;
import com.intellij.dvcs.DvcsRememberedInputs;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.dvcs.ui.DvcsCloneDialogComponent;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.VcsCloneComponent;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogComponentStateListener;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.containers.ContainerUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class RadCheckoutProvider implements CheckoutProvider {

    @Override
    public @NotNull VcsCloneComponent buildVcsCloneComponent(@NotNull Project project, @NotNull ModalityState modalityState,
                                                             @NotNull VcsCloneDialogComponentStateListener dialogStateListener) {
        return new VcsCloneComponentExt(project, "", new DvcsRememberedInputs(), dialogStateListener);
    }

    public void doCheckout(@NotNull Project project, @Nullable Listener listener) {

    }

    @Override
    public @NonNls String getVcsName() {
        return RadicleBundle.message("radicle");
    }

    public static class VcsCloneComponentExt extends DvcsCloneDialogComponent implements CloneProject {
        private final VcsCloneDialogComponentStateListener dialogComponentStateListener;
        private final Project project;
        private final TextFieldWithBrowseButton directoryField;
        private final TextFieldWithHistory urlField;
        private final JTextField radHomeField;
        private final JTextField radPathField;

        public VcsCloneComponentExt(@NotNull Project project, @NotNull String vcsDirectoryName, @NotNull DvcsRememberedInputs rememberedInputs,
                                    @NotNull VcsCloneDialogComponentStateListener dialogStateListener) {
            super(project, vcsDirectoryName, rememberedInputs, dialogStateListener);
            this.project = project;
            this.radHomeField = new JTextField();
            this.radPathField = new JTextField();
            this.urlField = new TextFieldWithHistory();
            this.directoryField = new TextFieldWithBrowseButton();
            this.directoryField.addBrowseFolderListener(RadicleBundle.message("selectDirectory"), "", null,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor());
            this.dialogComponentStateListener = dialogStateListener;
        }

        @NotNull
        @Override
        public JPanel getView() {
            var textFieldListener = new TextFieldListener();
            var mainPanel =  super.getView();
            mainPanel.removeAll();
            mainPanel.setLayout(new BorderLayout());

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.insets = new Insets(3, 3, 0, 0);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            gbc.fill = 0;
            panel.add(new JLabel(RadicleBundle.message("repositoryId")), gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.fill = GridConstraints.FILL_HORIZONTAL;
            urlField.addDocumentListener(textFieldListener);
            panel.add(urlField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            gbc.fill = 0;
            panel.add(new JLabel(RadicleBundle.message("directory")), gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weightx = 1;
            gbc.fill = GridConstraints.FILL_HORIZONTAL;
            directoryField.getTextField().getDocument().addDocumentListener(textFieldListener);
            panel.add(directoryField, gbc);

            if (!hasSettings()) {
                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.weightx = 0;
                gbc.fill = 0;
                panel.add(new JLabel(RadicleBundle.message("radPath")), gbc);

                gbc.gridx = 1;
                gbc.gridy = 2;
                gbc.weightx = 1;
                gbc.fill = GridConstraints.FILL_HORIZONTAL;
                radPathField.getDocument().addDocumentListener(textFieldListener);
                panel.add(radPathField, gbc);

                gbc.gridx = 0;
                gbc.gridy = 3;
                gbc.weightx = 0;
                gbc.fill = 0;
                panel.add(new JLabel(RadicleBundle.message("radHome")), gbc);

                gbc.gridx = 1;
                gbc.gridy = 3;
                gbc.weightx = 1;
                gbc.fill = GridConstraints.FILL_HORIZONTAL;
                radHomeField.getDocument().addDocumentListener(textFieldListener);
                panel.add(radHomeField, gbc);

                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    var radPath = detectRadPath();
                    if (Strings.isNullOrEmpty(radPath)) {
                        return;
                    }
                    radPathField.setText(radPath);
                    var radHome = detectRadHome(radPath);
                    if (!Strings.isNullOrEmpty(radHome)) {
                        radHomeField.setText(radHome);
                    }
                });
            }
            mainPanel.add(panel, BorderLayout.NORTH);
            return mainPanel;
        }

        @Override
        public void onComponentSelected(@NotNull VcsCloneDialogComponentStateListener dialogStateListener) {
            dialogComponentStateListener.onOkActionEnabled(enableCloneButton());
        }

        @NotNull
        @Override
        public List<ValidationInfo> doValidateAll() {
            var list = new ArrayList<ValidationInfo>();
            if (urlField != null && !isValidUrl(urlField.getText())) {
                ContainerUtil.addIfNotNull(list, new ValidationInfo(RadicleBundle.message("invalidId"), urlField));
            }
            if (directoryField != null && Strings.isNullOrEmpty(directoryField.getText())) {
                ContainerUtil.addIfNotNull(list, new ValidationInfo(RadicleBundle.message("fieldRequired"), directoryField));
            }
            if (directoryField != null && !Strings.isNullOrEmpty(directoryField.getText())) {
                ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkDirectory(directoryField.getText(), directoryField));
            }
            if (radPathField != null && Strings.isNullOrEmpty(radPathField.getText()) && !hasSettings()) {
                ContainerUtil.addIfNotNull(list, new ValidationInfo(RadicleBundle.message("fieldRequired"), radPathField));
            }
            if (radHomeField != null && Strings.isNullOrEmpty(radHomeField.getText()) && !hasSettings()) {
                ContainerUtil.addIfNotNull(list, new ValidationInfo(RadicleBundle.message("fieldRequired"), radHomeField));
            }
            return list;
        }

        private boolean isValidUrl(String url) {
            return url.startsWith("rad:");
        }

        /* This doClone is for 2022 version */
        public void doClone(@NotNull CheckoutProvider.Listener listener) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var isValid = isValidRadPath();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (isValid) {
                        CloneUtil.doClone(listener, project, this);
                    }
                });
            });
        }

        /* This doClone is for 2020 / 2021 versions */
        public void doClone(@NotNull Project pr, @NotNull CheckoutProvider.Listener listener) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var isValid = isValidRadPath();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (isValid) {
                        CloneUtil.doClone(listener, project, this);
                    }
                });
            });
        }

        @Override
        public String getId() {
            return urlField.getText();
        }

        @Override
        public String directory() {
            return directoryField.getText();
        }

        @Override
        public String getRadHome() {
            return radHomeField.getText();
        }

        @Override
        public String getPath() {
            return radPathField.getText();
        }

        private boolean isValidRadPath() {
            var rad = project.getService(RadicleProjectService.class);
            ProcessOutput output = rad.getVersion(radPathField.getText());
            var radInfo = output.getStdout();
            var isValid =  !Strings.isNullOrEmpty(radInfo) && radInfo.contains("rad");
            if (!isValid) {
                RadAction.showErrorNotification(project, "radCliError", RadicleBundle.message("radNotInstalled"));
            }
            return isValid;
        }

        private boolean hasSettings() {
            var rsh = new RadicleProjectSettingsHandler(project);
            var rs = rsh.loadSettings();
            return !Strings.isNullOrEmpty(rs.getRadHome()) && !Strings.isNullOrEmpty(rs.getPath());
        }

        private String detectRadPath() {
            var rad = project.getService(RadicleProjectService.class);
            return rad.detectRadPath();
        }

        private String detectRadHome(String radPath) {
            return project.getService(RadicleProjectService.class).detectRadHome(radPath);
        }

        private boolean enableCloneButton() {
            var enable = false;
            if (hasSettings()) {
                enable = !Strings.isNullOrEmpty(urlField.getText()) && !Strings.isNullOrEmpty(directoryField.getText());
            } else {
                enable = !Strings.isNullOrEmpty(radPathField.getText()) && !Strings.isNullOrEmpty(radHomeField.getText()) &&
                        !Strings.isNullOrEmpty(urlField.getText()) && !Strings.isNullOrEmpty(directoryField.getText());
            }
            return enable;
        }

        private class TextFieldListener extends DocumentAdapter {

            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                dialogComponentStateListener.onOkActionEnabled(enableCloneButton());
            }
        }
    }
}
