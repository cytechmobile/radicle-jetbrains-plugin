package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.messages.MessageBusConnection;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadiclePullAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPull;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSync;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class ActionsTest extends HeavyPlatformTestCase {
    private static final Logger logger = Logger.getInstance(ActionsTest.class);
    private final BlockingQueue<Notification> notificationsQueue = new LinkedBlockingQueue<>();
    public static final String radVersion = "0.6.0";
    public static final String radPath = "/usr/bin/rad";
    public static final String wsl = "wsl";
    private static final String remoteName = "testRemote";
    private RadicleSettingsHandler radicleSettingsHandler;
    private String remoteRepoPath;
    private GitRepository repository;

    private MessageBusConnection mbc;
    private RadStub radStub;

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
        addSeedNodeInConfig(remoteRepoPath);

        mbc = getProject().getMessageBus().connect();
        mbc.setDefaultHandler(
                (event1, params) -> {
                    assertThat(params).hasSize(1);
                    assertThat(params[0]).isInstanceOf(Notification.class);
                    Notification n = (Notification) params[0];
                    logger.warn("captured notification: " + n);
                    notificationsQueue.add(n);
                });
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

    @Test
    public void cliConfiguredTest() throws InterruptedException {
        radicleSettingsHandler.savePath("");
        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("radCliPathMissingText"));

        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("radCliPathMissingText"));

        radicleSettingsHandler.savePath(radPath);
    }

    @Test
    public void radPullTest() throws InterruptedException {
        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());
        var result = rpa.getUpdateCountDown().await(10, TimeUnit.SECONDS);
        assertThat(result).isTrue();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadPull().getNotificationSuccessMessage());
    }

    @Test
    public void radSyncTest() throws InterruptedException {
        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        var result = rsa.getUpdateCountDown().await(10, TimeUnit.SECONDS);
        assertThat(result).isTrue();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("sync");
        var not = notificationsQueue.poll(10,TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadSync().getNotificationSuccessMessage());
    }

    public static void assertCmd(GeneralCommandLine cmd) {
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(wsl);
            var path = cmd.getParametersList().get(0);
            assertThat(path).isEqualTo(radPath);
        } else {
            assertThat(cmd.getExePath()).isEqualTo(radPath);
        }
    }

    public void addSeedNodeInConfig(String repoPath) {
        try {
            final String gitConfigFile = "/.git/config";
            Path filePath = Path.of(repoPath + gitConfigFile);
            String content = Files.readString(filePath);
            content += "\n[rad]\n\tseed=https://maple.radicle.garden/";
            Files.writeString(filePath, content);
        } catch (Exception e) {
            logger.warn("unable to write seed node in config file");
        }
    }
}
