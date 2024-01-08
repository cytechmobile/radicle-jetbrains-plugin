package network.radicle.jetbrains.radiclejetbrainsplugin.services.auth;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.serviceContainer.NonInjectable;
import com.sshtools.common.publickey.SshKeyUtils;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.IdentityDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String RADICLE_KEY_PATH = "/keys/radicle";

    private final RadicleProjectSettingsHandler projectSettingsHandler;
    private final RadicleProjectService radicleProjectService;
    private IdentityDialog identityDialog;
    private Project project;

    public AuthService(Project project) {
        this(new RadicleProjectSettingsHandler(project), project.getService(RadicleProjectService.class));
        this.project = project;
    }

    @NonInjectable
    public AuthService(Project project, IdentityDialog dialog) {
        this(new RadicleProjectSettingsHandler(project), project.getService(RadicleProjectService.class));
        this.project = project;
        this.identityDialog = dialog;
    }

    @NonInjectable
    public AuthService(RadicleProjectSettingsHandler projectSettingsHandler, RadicleProjectService radicleProjectService) {
        this.projectSettingsHandler = projectSettingsHandler;
        this.radicleProjectService = radicleProjectService;
    }

    public String authenticate(RadicleProjectApi.Session session) {
        var sh = new RadicleProjectSettingsHandler(project);
        var password = sh.getPassword(session.publicKey());
        //If we don't have the password then ask it from the user
        if (password == null) {
            var dialogTitle = RadicleBundle.message("unlockIdentity");
            var dialogData = showIdentityDialog(dialogTitle, true, this.identityDialog);
            if (dialogData == null) {
                return null;
            }
            password = dialogData.passphrase();
        }
        return sign(session, password);
    }

    public IdentityDialog.IdentityDialogData showIdentityDialog(String title, boolean hasIdentity, IdentityDialog dialog) {
        var latch = new CountDownLatch(1);
        AtomicBoolean okButton = new AtomicBoolean(false);
        AtomicReference<IdentityDialog> resp = new AtomicReference<>(null);
        ApplicationManager.getApplication().invokeLater(() -> {
            var myDialog = dialog == null ? new IdentityDialog() : dialog;
            resp.set(myDialog);
            myDialog.setTitle(title);
            myDialog.hasIdentity(hasIdentity);
            okButton.set(myDialog.showAndGet());
            latch.countDown();
        }, ModalityState.any());
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("error awaiting update latch!", e);
            return null;
        }
        if (okButton.get()) {
            return resp.get().getData();
        }
        return null;
    }

    protected String sign(RadicleProjectApi.Session session, String password) {
        try {
            var settings = projectSettingsHandler.loadSettings();
            var keyPath = getRadicleKey(settings.getRadHome());
            if (keyPath == null) {
                logger.debug("Failed to sign the session. Invalid radicle key path");
                return null;
            }
            var kp = SshKeyUtils.getPrivateKey(new File(keyPath), password);
            var dataToSign = session.sessionId() + ":" + session.publicKey();
            var signedData = kp.getPrivateKey().sign(dataToSign.getBytes(StandardCharsets.UTF_8));
            var encodedSignedData = Multibase.encode(Multibase.Base.Base58BTC, signedData);
            //Save user password for later use
            projectSettingsHandler.savePassphrase(session.publicKey(), password);
            return encodedSignedData;
        } catch (Exception e) {
            logger.debug("Failed to sign the session. Error: {}", e.getMessage());
            return null;
        }
    }

    private String getRadicleKey(String radHome) {
        var radicleKeyPath = radHome + RADICLE_KEY_PATH;
        if (SystemInfo.isWindows) {
            return radicleProjectService.getWindowsPath(radicleKeyPath);
        }
        return radicleKeyPath;
    }
}
