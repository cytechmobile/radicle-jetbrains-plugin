package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAuth;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPath;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.IdentityDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import static network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction.showNotification;

public class RadicleSettingsView  implements SearchableConfigurable {
    private static final DefaultArtifactVersion minVersion = new DefaultArtifactVersion("0.8.0");
    private static final DefaultArtifactVersion maxVersion = new DefaultArtifactVersion("0.9.0");
    public static final String ID = RadicleBundle.message("radicle");

    protected TextFieldWithBrowseButton radPathField;
    protected JPanel mainPanel;
    private JButton testButton;
    private JLabel radVersionLabel;
    private JLabel enforceVersionLabel;
    protected TextFieldWithBrowseButton radHomeField;
    private JButton testRadHomeButton;
    private JLabel msgLabel;
    private RadicleProjectSettings projectSettings;
    private final RadicleProjectSettingsHandler radicleSettingsHandler;
    private JButton seedNodeApiUrlTestBtn;
    private JLabel seedNodeApiUrlLabel;
    private JLabel seedNodeApiUrlMsgLabel;
    private JTextField seedNodeApiUrl;
    private IdentityDialog identityDialog;
    private RadDetails radDetails;
    private final Project myProject;

    public RadicleSettingsView(Project project) {
        super();
        myProject = project;
        this.radicleSettingsHandler = new RadicleProjectSettingsHandler(project);
        this.projectSettings = this.radicleSettingsHandler.loadSettings();
        initComponents();
    }

    public RadicleSettingsView(IdentityDialog identityDialog, Project project) {
        this(project);
        this.identityDialog = identityDialog;
    }

    protected String getRadVersion() {
        var rad = myProject.getService(RadicleProjectService.class);
        if (Strings.isNullOrEmpty(getPathFromTextField(radPathField))) {
            return "";
        }
        ProcessOutput output = rad.getVersion(getPathFromTextField(radPathField));
        var radInfo = output.getStdout();
        if (!Strings.isNullOrEmpty(radInfo) && radInfo.contains("rad")) {
            var radInfoParts = radInfo.split(" ");
            return radInfoParts.length > 1 ? radInfoParts[1] : "";
        }
        return "";
    }

    protected String getRadPath() {
        var rad = myProject.getService(RadicleProjectService.class);
        ProcessOutput output = rad.radPath();
        if (!RadAction.isSuccess(output)) {
            return "";
        }
        var pathInfo = output.getStdoutLines();
        // which command return empty and where command return INFO if the os cant find the program path
        if (pathInfo.size() > 0 && !Strings.isNullOrEmpty(pathInfo.get(0)) && !pathInfo.get(0).contains("INFO")) {
            return pathInfo.get(0);
        }
        return "";
    }

    protected String getRadHome() {
        var radHome = new RadPath(myProject);
        var output = radHome.perform();
        if (!RadAction.isSuccess(output)) {
            return "";
        }
        return output.getStdout().replace("\n", "");
    }

    private boolean isRadHomeValidPath(String radPath, String radHome) {
        var radSelf = new RadSelf(radHome, radPath, myProject);
        var output = radSelf.perform();
        return RadAction.isSuccess(output);
    }

    private void unlockOrCreateIdentity() {
        msgLabel.setText("");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radHome = getPathFromTextField(radHomeField);
            var radPath = getPathFromTextField(radPathField);
            var radSelf = new RadSelf(radHome, radPath, myProject);
            var output = radSelf.perform();
            var lines = output.getStdoutLines(true);
            radDetails = new RadDetails(lines);
            var rad = myProject.getService(RadicleProjectService.class);
            var isIdentityUnlocked = rad.isIdentityUnlocked(radDetails.keyHash);
            /* If radSelf executed with exit code 0 then we have an identity !! */
            var success = RadAction.isSuccess(output);
            ApplicationManager.getApplication().invokeLater(() -> {
                /* if there is identity and its unlocked just update the label with node id */
                if (success && isIdentityUnlocked) {
                    msgLabel.setText(radDetails.did);
                    return;
                }
                /* if there is no identity or unlocked identity show passphrase dialog */
                var title = !success ? RadicleBundle.message("newIdentity") :
                        RadicleBundle.message("unlockIdentity");
                var dialog = this.identityDialog == null ? new IdentityDialog() : this.identityDialog;
                dialog.setTitle(title);
                var okButton = dialog.showAndGet();
                if (okButton) {
                    var action = !success ? RadAuth.RadAuthAction.CREATE_IDENTITY :
                            RadAuth.RadAuthAction.UNLOCKED_IDENTITY;
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var radAuth = new RadAuth(dialog.passphraseField.getText(), radHome, radPath, action, myProject);
                        var radAuthOutput = radAuth.perform();
                        var isSuccess = RadAction.isSuccess(radAuthOutput);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            var msg = isSuccess ? radAuth.getNotificationSuccessMessage() : radAuthOutput.getStderr();
                            this.msgLabel.setText("<html>" + msg + "</html>");
                        }, ModalityState.any());
                    });
                }
            }, ModalityState.any());
        });
    }

    private void updateRadVersionLabel() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var msg = RadicleBundle.message("radNotInstalled");
            var version = getRadVersion();
            if (!Strings.isNullOrEmpty(version)) {
                msg = RadicleBundle.message("radVersion") + " " + version;
            }
            String finalMsg = msg;
            ApplicationManager.getApplication().invokeLater(() -> {
                radVersionLabel.setText(finalMsg);
                showHideEnforceLabel(version);
            }, ModalityState.any());
        });
    }

    private boolean isValidNodeApi() {
        var api = new ProjectApi();
        var resp = api.checkApi(new SeedNode(seedNodeApiUrl.getText()));
        return resp != null && Strings.isNullOrEmpty(resp.errorMessage());
    }

    private void checkSeedNodeApiUrl() {
        var api = new ProjectApi();
        var resp = api.checkApi(new SeedNode(seedNodeApiUrl.getText()));
        if (!isValidNodeApi()) {
            seedNodeApiUrlMsgLabel.setText("<html>" +
                    RadicleBundle.message("seedNodeCheckError") +
                    (resp == null ? "" :  "<br>" + resp.errorMessage()) +
                    "</html>");
        } else {
            seedNodeApiUrlMsgLabel.setText(RadicleBundle.message("seedNodeCheckSuccess", "", resp.id(), resp.version()));
        }
    }

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return ID;
    }

    @Override
    @Nullable
    public JComponent createComponent() {
        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        this.projectSettings = this.radicleSettingsHandler.loadSettings();
        this.projectSettings = this.radicleSettingsHandler.loadSettings();
        var selectedPath = getPathFromTextField(radPathField);
        var selectedRadHome = getPathFromTextField(radHomeField);
        return !selectedPath.equals(this.projectSettings.getPath()) ||
                !selectedRadHome.equals(this.projectSettings.getRadHome()) ||
                !getSelectedNodeUrl().equals(this.projectSettings.getSeedNode().url);
    }

    @Override
    public void apply() {
        var path = getPathFromTextField(radPathField);
        var radHomePath = getPathFromTextField(radHomeField);
        var nodeUrl = getSelectedNodeUrl();
        radicleSettingsHandler.saveRadHome(radHomePath);
        radicleSettingsHandler.savePath(path);
        radicleSettingsHandler.saveSeedNode(nodeUrl);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var version = getRadVersion();
            var isVersionSupported = isCompatibleVersion(version);
            var isRadHomeValidPath = isRadHomeValidPath(path, radHomePath);
            var isValidNodeApi = isValidNodeApi();
            /* Rad version and home path is valid we don't have to show a notification warning */
            if (isVersionSupported && isRadHomeValidPath && isValidNodeApi) {
                return;
            }
            /* Show a notification warning that RAD_HOME or rad executable are invalid */
            var title = "";
            if (!isVersionSupported) {
                title = RadicleBundle.message("invalidRad");
            } else if (!isRadHomeValidPath) {
                title = RadicleBundle.message("noNode");
            } else {
                title = RadicleBundle.message("invalidSeedApi");
            }

            var msg = "";
            if (!isVersionSupported) {
                msg = RadicleBundle.message("radNotInstalled");
            } else if (!isRadHomeValidPath) {
                msg = RadicleBundle.message("createNode");
            } else {
                msg = RadicleBundle.message("invalidSeedApiMsg");
            }
            String finalTitle = title;
            String finalMsg = msg;
            ApplicationManager.getApplication().invokeLater(() ->
                    showNotification(myProject, finalTitle, finalMsg, NotificationType.WARNING,
                            List.of(new RadAction.ConfigureRadCliNotificationAction(myProject,
                                    RadicleBundle.lazyMessage("configure")))), ModalityState.any());
        });
    }

    private void initListeners() {
        testButton.addActionListener(e -> this.updateRadVersionLabel());
        testRadHomeButton.addActionListener(e -> this.unlockOrCreateIdentity());
        seedNodeApiUrlTestBtn.addActionListener(e -> this.checkSeedNodeApiUrl());

        var radPathFieldListener = new FieldListener(radPathField, AutoDetect.Type.RAD_EXE_PATH);
        var radHomeFieldListener = new FieldListener(radHomeField, AutoDetect.Type.RAD_HOME);
        radPathField.getTextField().addFocusListener(radPathFieldListener);
        radHomeField.getTextField().addFocusListener(radHomeFieldListener);
        radPathField.getTextField().getDocument().addDocumentListener(radPathFieldListener);
        radHomeField.getTextField().getDocument().addDocumentListener(radHomeFieldListener);
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        radPathField = null;
        testButton = null;
        radVersionLabel = null;
    }

    private String getPathFromTextField(TextFieldWithBrowseButton field) {
        var path = field != null ? field.getText() : "";
        if (path.contains(RadicleBundle.message("autoDetected"))) {
            path = path.split(":")[1].trim();
        }
        return path;
    }

    private String getSelectedNodeUrl() {
        return Strings.nullToEmpty(seedNodeApiUrl.getText());
    }

    private void initComponents() {
        this.msgLabel.setPreferredSize(new Dimension(200, this.msgLabel.getHeight()));
        radPathField.setText(this.projectSettings.getPath());
        radPathField.addBrowseFolderListener(RadicleBundle.message("selectExecutable"), "", null,
                new FileChooserDescriptor(true, false, false, false, false, false));

        radHomeField.setText(this.projectSettings.getRadHome());
        radHomeField.addBrowseFolderListener(RadicleBundle.message("selectExecutable"), "", null,
                new FileChooserDescriptor(false, true, false, false, false, false));

        enforceVersionLabel.setVisible(false);
        enforceVersionLabel.setText(RadicleBundle.message("unSupportedCliVer"));
        enforceVersionLabel.setFont(enforceVersionLabel.getFont().deriveFont(Font.BOLD));
        /* Try to autodetect RAD_HOME and RAD PATH */
        if (Strings.isNullOrEmpty(getPathFromTextField(radPathField))) {
            var autoDetect = new AutoDetect(radPathField, AutoDetect.Type.RAD_EXE_PATH);
            autoDetect.detectAndUpdateField();
        }
        if (Strings.isNullOrEmpty(getPathFromTextField(radHomeField))) {
            var autoDetect = new AutoDetect(radHomeField, AutoDetect.Type.RAD_HOME);
            autoDetect.detectAndUpdateField();
        }
        seedNodeApiUrl.setText(this.projectSettings.getSeedNode().url);
        initListeners();
        /* Show a warning label if the rad version is incompatible */
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var version = getRadVersion();
            ApplicationManager.getApplication().invokeLater(() -> {
                showHideEnforceLabel(version);
            }, ModalityState.any());
        });
    }

    private boolean isCompatibleVersion(String v) {
        if (Strings.isNullOrEmpty(v)) {
            return false;
        }
        var version = new DefaultArtifactVersion(v.replace("\n", "").trim());
        return version.compareTo(minVersion) >= 0 && version.compareTo(maxVersion) <= 0;
    }

    private void showHideEnforceLabel(String radVersion) {
        if (Strings.isNullOrEmpty(radVersion)) {
            return;
        }
        enforceVersionLabel.setVisible(false);
        if (!isCompatibleVersion(radVersion)) {
            enforceVersionLabel.setVisible(true);
        }
    }

    public JLabel getRadVersionLabel() {
        return radVersionLabel;
    }

    public JButton getTestButton() {
        return testButton;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public TextFieldWithBrowseButton getPathField() {
        return radPathField;
    }

    public TextFieldWithBrowseButton getHomeField() {
        return radHomeField;
    }

    public JButton getRadHomeTestButton() {
        return testRadHomeButton;
    }

    public RadDetails getRadDetails() {
        return radDetails;
    }

    private class AutoDetect {
        private final TextFieldWithBrowseButton textField;
        private final Type type;
        public enum Type {
            RAD_EXE_PATH, RAD_HOME
        }
        public AutoDetect(TextFieldWithBrowseButton textField, Type type) {
            this.textField = textField;
            this.type = type;
        }

        private String getPath() {
            return this.type == Type.RAD_EXE_PATH ? getRadPath() : getRadHome();
        }

        public void detectAndUpdateField() {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var path = getPath();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!Strings.isNullOrEmpty(path) && textField != null) {
                        textField.getTextField().setForeground(JBColor.GRAY);
                        textField.setText(RadicleBundle.message("autoDetected") + path);
                    }
                }, ModalityState.any());
            });
        }
    }

    private class FieldListener extends DocumentAdapter implements FocusListener {
        private final TextFieldWithBrowseButton textField;
        private final AutoDetect.Type type;

        public FieldListener(TextFieldWithBrowseButton textField, AutoDetect.Type type) {
            this.textField = textField;
            this.type = type;
        }

        @Override
        public void focusGained(FocusEvent e) {
            //Clear field when the user click to write
            if (textField.getText().contains(RadicleBundle.message("autoDetected"))) {
                textField.setText("");
                textField.getTextField().setForeground(JBColor.BLACK);
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            //Auto-detect path
            if (getPathFromTextField(textField).isEmpty()) {
                var autoDetect = new AutoDetect(textField, type);
                autoDetect.detectAndUpdateField();
            }
        }

        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            /* Change the color of the field when the user select a path from the dialog */
            if (!textField.getText().contains(RadicleBundle.message("autoDetected"))) {
                textField.getTextField().setForeground(JBColor.BLACK);
            }
        }
    }
}
