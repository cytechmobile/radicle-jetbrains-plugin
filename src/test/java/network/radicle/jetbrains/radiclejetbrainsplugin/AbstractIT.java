package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.messages.MessageBusConnection;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractIT extends HeavyPlatformTestCase {
    private static final Logger logger = Logger.getInstance(ActionsTest.class);
    public final BlockingQueue<Notification> notificationsQueue = new LinkedBlockingQueue<>();
    public static final String radVersion = "0.6.0";
    public static final String radPath = "/usr/bin/rad";
    public static final String wsl = "wsl";
    protected static final String remoteName = "testRemote";
    protected RadicleSettingsHandler radicleSettingsHandler;
    protected String remoteRepoPath;
    protected GitRepository repository;


    private MessageBusConnection mbc,applicationMbc;
    public RadStub radStub;

    @Before
    public void before() throws IOException {
        /* initialize a git repository */
        remoteRepoPath = Files.createTempDirectory(remoteName).toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
        repository = GitTestUtil.createGitRepository(super.getProject(),remoteRepoPath);

        /* set rad path */
        radicleSettingsHandler = new RadicleSettingsHandler();
        radicleSettingsHandler.loadSettings();
        radicleSettingsHandler.savePath(radPath);

        /* initialize rad stub service */
        radStub = RadStub.replaceRadicleApplicationService(this);

        /* add seed node in config */
        addSeedNodeInConfig();
        initializeProject();
        applicationMbc =  ApplicationManager.getApplication().getMessageBus().connect();
        mbc = getProject().getMessageBus().connect();
        mbc.setDefaultHandler(
                (event1, params) -> {
                    assertThat(params).hasSize(1);
                    assertThat(params[0]).isInstanceOf(Notification.class);
                    Notification n = (Notification) params[0];
                    logger.warn("captured notification: " + n);
                    notificationsQueue.add(n);
                });
        applicationMbc.setDefaultHandler(
                (event1, params) -> {
                    assertThat(params).hasSize(1);
                    assertThat(params[0]).isInstanceOf(Notification.class);
                    Notification n = (Notification) params[0];
                    logger.warn("captured notification: " + n);
                    notificationsQueue.add(n);
                });
        applicationMbc.subscribe(Notifications.TOPIC);
        mbc.subscribe(Notifications.TOPIC);
        logger.warn("created message bus connection and subscribed to notifications: {}" + mbc);
    }

    @After
    public final void after() {
        if(mbc != null) {
            mbc.disconnect();
        }
        try {
            repository.dispose();
        } catch (Exception e) {
            logger.warn("error disposing git repo");
        }
    }

    public static void assertCmd(GeneralCommandLine cmd) {
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(wsl);
            assertThat("bash").isEqualTo(cmd.getParametersList().get(0));
            assertThat("-ic").isEqualTo(cmd.getParametersList().get(1));
            assertThat(cmd.getParametersList().get(2)).contains(radPath);
        } else {
            assertThat(cmd.getExePath()).isEqualTo(radPath);
        }
    }

    protected void removeRemoteRadUrl() {
        try {
            GitConfigUtil.setValue(super.getProject(),repository.getRoot(), "remote.rad.url","");
        } catch (Exception e) {
            logger.warn("unable to write remote rad url in config file");
        }
    }

    protected void initializeProject() {
        try {
            GitConfigUtil.setValue(super.getProject(),repository.getRoot(),
                    "remote.rad.url","rad://hrrkbjxncopa15doj7qobxip8fotbcemjro4o.git");
        } catch (Exception e) {
            logger.warn("unable to write remote rad url in config file");
        }
    }

    protected void removeSeedNodeFromConfig() {
        try {
            GitConfigUtil.setValue(super.getProject(),repository.getRoot(), "rad.seed","");
        } catch (Exception e) {
            logger.warn("unable to remove seed node from config file");
        }
    }

    protected void addSeedNodeInConfig() {
        try {
            GitConfigUtil.setValue(super.getProject(),repository.getRoot(), "rad.seed",
                    "https://maple.radicle.garden");
        } catch (Exception e) {
            logger.warn("unable to write seed node in config file");
        }
    }

}
