package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadiclePullAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPull;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class ActionsTest extends HeavyPlatformTestCase {
    private static final Logger logger = Logger.getInstance(ActionsTest.class);
    private final BlockingQueue<Notification> notificationsQueue = new LinkedBlockingQueue<>();
    private static final String radPath = "/usr/bin/rad";
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
        final var app = ApplicationManager.getApplication();
        radStub = new RadStub();
        ServiceContainerUtil.replaceService(app, RadicleApplicationService.class, radStub, getTestRootDisposable());

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
    public final void after() throws Exception {
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
    public void radPullTest() throws InterruptedException {
        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());
        var result = rpa.getUpdateCountDown().await(10, TimeUnit.SECONDS);
        assertThat(result).isTrue();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertThat(cmd.getExePath()).isEqualTo(radPath);
        assertThat(cmd.getCommandLineString()).contains("pull");

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadPull().getNotificationSuccessMessage());
    }
}
