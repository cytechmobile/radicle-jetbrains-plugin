package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.IdentityDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction.showNotification;

public class RadicleSettingsView  implements SearchableConfigurable {
    private static final Version MIN_VERSION = new Version("0.8.0");
    private static final Version MAX_VERSION = new Version("1.0.0");
    private static final Logger logger = Logger.getInstance(RadicleSettingsView.class);
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
    private javax.swing.JTextArea seedNodeApiUrlMsgLabel;
    private JTextField seedNodeApiUrl;
    private JTextPane textPane1;
    private IdentityDialog identityDialog;
    private RadDetails radDetails;
    private final Project myProject;
    private final CountDownLatch myLatch = new CountDownLatch(1);
    protected CountDownLatch init = new CountDownLatch(1);
    protected AutoDetect autoDetectRadPath;
    protected AutoDetect autoDetectRadHome;

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
        return rad.detectRadPath();
    }

    protected String getRadHome() {
        var radPath = getPathFromTextField(radPathField);
        return myProject.getService(RadicleProjectService.class).detectRadHome(radPath);
    }

    private boolean isRadHomeValidPath(String radPath, String radHome, IdentityDialog dialog) {
        var radSelf = new RadSelf(radHome, radPath, myProject);
        radSelf.askForIdentity(false);
        var output = radSelf.perform(dialog);
        return RadAction.isSuccess(output);
    }

    private void unlockOrCreateIdentity() {
        msgLabel.setText("");
        var dialog = this.identityDialog == null ? new IdentityDialog() : this.identityDialog;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radHome = getPathFromTextField(radHomeField);
            var radPath = getPathFromTextField(radPathField);
            var radSelf = new RadSelf(radHome, radPath, myProject);
            var output = radSelf.perform(radHome, radPath, dialog);
            var lines = output.getStdoutLines(true);
            radDetails = new RadDetails(lines);
            logger.warn("got rad details: " + radDetails.did);
            var api = myProject.getService(RadicleProjectApi.class);
            if (api != null) {
                api.resetCurrentIdentity();
            }
            myLatch.countDown();
            // check if rad home is non-default
            if (Strings.isNullOrEmpty(autoDetectRadHome.detected)) {
                autoDetectRadHome.detect();
            }
            String warning = "";
            if (!Strings.isNullOrEmpty(autoDetectRadHome.detected) && !autoDetectRadHome.detected.equals(radHome)) {
                //show warnings
                String message = RadicleBundle.message(SystemInfo.isWindows ? "nonDefaultHomeWarningWindows" : "nonDefaultHomeWarning");
                warning = "<p>" + message + "</p>";
            }
            StringBuilder message = new StringBuilder("<html>");
            if (RadAction.isSuccess(output)) {
                message.append(radDetails.did);
            } else {
                message.append(output.getStderr());
            }
            message.append(warning).append("</html>");
            ApplicationManager.getApplication().invokeLater(() -> this.msgLabel.setText(message.toString()), ModalityState.any());
            ApplicationManager.getApplication().invokeLater(() -> {
                if (this.textPane1 != null) {
                    this.textPane1.setText(message.toString());
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

    private RadicleProjectApi.SeedNodeInfo checkApi() {
        // return new RadicleProjectApi.SeedNodeInfo(null, null, null, "my error");
        var api = myProject.getService(RadicleProjectApi.class);
        return api.checkApi(new SeedNode(seedNodeApiUrl.getText()), true);
    }

    private boolean isValidNodeApi() {
        return isValidNodeApi(checkApi());
    }

    private boolean isValidNodeApi(RadicleProjectApi.SeedNodeInfo resp) {
        return Utils.isValidNodeApi(resp);
    }

    private boolean isCompatibleNodeApi(RadicleProjectApi.SeedNodeInfo resp) {
        if (resp == null || Strings.isNullOrEmpty(resp.apiVersion())) {
            return false;
        }
        return resp.apiVersion().startsWith("0.1.");
    }

    private void checkSeedNodeApiUrl() {
        var resp = checkApi();
        if (!isValidNodeApi(resp)) {
            seedNodeApiUrlMsgLabel.setText(RadicleBundle.message("seedNodeCheckError") + (resp == null ? "" :  "\n" + resp.errorMessage()));
        } else {
            String compat = "";
            if (!isCompatibleNodeApi(resp)) {
                compat = "\n" + RadicleBundle.message("seedNodeCheckIncompatibleApiVersion", "0.1.x");
            }
            seedNodeApiUrlMsgLabel.setText(RadicleBundle.message("seedNodeCheckSuccess", resp.id(), resp.version(), resp.apiVersion()) + compat);
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
        var dialog = this.identityDialog == null ? new IdentityDialog() : this.identityDialog;
        var path = getPathFromTextField(radPathField);
        var radHomePath = getPathFromTextField(radHomeField);
        var nodeUrl = getSelectedNodeUrl();
        radicleSettingsHandler.saveRadHome(radHomePath);
        radicleSettingsHandler.savePath(path);
        radicleSettingsHandler.saveSeedNode(nodeUrl);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var version = getRadVersion();
            var isVersionSupported = isCompatibleVersion(version);
            var isRadHomeValidPath = isRadHomeValidPath(path, radHomePath, dialog);
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

        var radPathFieldListener = new FieldListener(radPathField, autoDetectRadPath);
        var radHomeFieldListener = new FieldListener(radHomeField, autoDetectRadHome);
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
        radPathField.setText(this.projectSettings.getPath());
        radPathField.addBrowseFolderListener(RadicleBundle.message("selectExecutable"), "", null,
                new FileChooserDescriptor(true, false, false, false, false, false));

        radHomeField.setText(this.projectSettings.getRadHome());
        radHomeField.addBrowseFolderListener(RadicleBundle.message("selectExecutable"), "", null,
                new FileChooserDescriptor(false, true, false, false, false, false));

        enforceVersionLabel.setVisible(false);
        enforceVersionLabel.setText(RadicleBundle.message("unSupportedCliVer"));
        enforceVersionLabel.setFont(enforceVersionLabel.getFont().deriveFont(Font.BOLD));
        // Try to autodetect RAD_HOME and RAD PATH
        autoDetectRadPath = new AutoDetect(AutoDetect.Type.RAD_EXE_PATH);
        autoDetectRadHome = new AutoDetect(AutoDetect.Type.RAD_HOME);
        final var latch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            autoDetectRadPath.detect();
            autoDetectRadHome.detect();
            latch.countDown();
        });
        var hasRadPath = !Strings.isNullOrEmpty(getPathFromTextField(radPathField));
        var hasRadHome = !Strings.isNullOrEmpty(getPathFromTextField(radHomeField));
        if (!hasRadPath || !hasRadHome) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    //We have to wait for RAD_PATH, in order to find RAD_HOME
                    latch.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.warn("Unable to wait for rad path/home");
                }
               ApplicationManager.getApplication().invokeLater(() -> {
                   if (!hasRadPath) {
                       autoDetectRadPath.updateField(radPathField, autoDetectRadPath.detected);
                   }
                   if (!hasRadHome) {
                       autoDetectRadHome.updateField(radHomeField, autoDetectRadHome.detected);
                   }
               }, ModalityState.any());
            });
        }

        seedNodeApiUrl.setText(this.projectSettings.getSeedNode().url);
        seedNodeApiUrlMsgLabel.setLineWrap(true);
        seedNodeApiUrlMsgLabel.setOpaque(false);
        initListeners();
        // Show a warning label if the rad version is incompatible
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var version = getRadVersion();
            init.countDown();
            ApplicationManager.getApplication().invokeLater(() -> showHideEnforceLabel(version), ModalityState.any());
        });
    }

    private boolean isCompatibleVersion(String v) {
        if (Strings.isNullOrEmpty(v)) {
            return false;
        }
        try {
            //Get only the numeric value of the version
            var parts = v.replace("\n", "").trim().split("-");
            var numeric = parts[0];
            var radVersion = new Version(numeric);
            return radVersion.compareTo(MIN_VERSION) >= 0 && radVersion.compareTo(MAX_VERSION) <= 0;
        } catch (Exception e) {
            return false;
        }
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

    public CountDownLatch getLatch() {
        return myLatch;
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

    public class AutoDetect {
        private final Type type;
        protected String detected;

        public enum Type {
            RAD_EXE_PATH, RAD_HOME
        }

        public AutoDetect(Type type) {
            this.type = type;
        }

        public String detect() {
            detected = getPath();
            return detected;
        }

        public void updateField(TextFieldWithBrowseButton textField, String path) {
            if (!Strings.isNullOrEmpty(path) && textField != null) {
                textField.getTextField().setForeground(JBColor.GRAY);
                textField.setText(RadicleBundle.message("autoDetected") + path);
            }
        }

        private String getPath() {
            return this.type == Type.RAD_EXE_PATH ? getRadPath() : getRadHome();
        }
    }

    public class FieldListener extends DocumentAdapter implements FocusListener {
        private final TextFieldWithBrowseButton textField;
        private final AutoDetect autoDetect;

        public FieldListener(TextFieldWithBrowseButton textField, AutoDetect autoDetect) {
            this.textField = textField;
            this.autoDetect = autoDetect;
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
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                   var path = autoDetect.detect();
                   ApplicationManager.getApplication().invokeLater(() -> autoDetect.updateField(textField, path), ModalityState.any());
                });
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

    public static class Version implements Comparable<Version> {
        private final String version;

        public Version(String version) {
            if (version == null) {
                throw new IllegalArgumentException("Version can not be null");
            }
            if (!version.matches("[0-9]+(\\.[0-9]+)*")) {
                throw new IllegalArgumentException("Invalid version format");
            }
            this.version = version;
        }

        @Override
        public int compareTo(@NotNull Version ver) {
            String[] thisParts = this.version.split("\\.");
            String[] verParts = ver.version.split("\\.");
            int length = Math.max(thisParts.length, verParts.length);
            for (var i = 0; i < length; i++) {
                var thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
                var thatPart = i < verParts.length ? Integer.parseInt(verParts[i]) : 0;
                if (thisPart < thatPart) {
                    return -1;
                }
                if (thisPart > thatPart) {
                    return 1;
                }
            }
            return 0;
        }
    }
}
