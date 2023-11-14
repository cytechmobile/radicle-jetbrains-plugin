package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.testFramework.CoroutineKt;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import git4idea.GitCommit;
import git4idea.config.GitConfigUtil;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public abstract class AbstractIT extends HeavyPlatformTestCase {
    private static final Logger logger = Logger.getInstance(AbstractIT.class);
    public final BlockingQueue<Notification> notificationsQueue = new LinkedBlockingQueue<>();
    public static final String RAD_VERSION = "0.6.1";
    public static final String RAD_PATH = "/usr/bin/rad";
    public static final String RAD_HOME = "/home/test/radicle";
    public static final String RAD_HOME1 = "/test2/secondInstallation";
    public static final String WSL = "wsl";
    protected static final String REMOTE_NAME = "testRemote";
    protected static final String REMOTE_NAME_1 = "testRemote1";
    public static final String PROJECTS_URL = "/projects";
    public static final String PATCHES_URL = "/patches";
    public static final String ISSUES_URL = "/issues";
    public static final String SESSIONS_URL = "/sessions";
    protected RadicleProjectSettingsHandler radicleProjectSettingsHandler;
    protected String remoteRepoPath;
    protected String remoteRepoPath1;
    protected GitRepository firstRepo;
    protected GitRepository secondRepo;

    private MessageBusConnection mbc;
    private MessageBusConnection applicationMbc;
    public RadStub radStub;
    public List<GitCommit> commitHistory;

    @Before
    public void before() throws IOException, VcsException {
        /* initialize a git repository */
        remoteRepoPath = Files.createTempDirectory(REMOTE_NAME).toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
        firstRepo = GitTestUtil.createGitRepository(super.getProject(), remoteRepoPath);

        // Create a commit
        var fileToChange = new File(firstRepo.getRoot().getPath() + "/initial_file.txt");
        GitTestUtil.writeToFile(fileToChange, "Hello");
        GitExecutor.addCommit("my message");

        // Create second a commit
        fileToChange = new File(firstRepo.getRoot().getPath() + "/initial_file_2.txt");
        GitTestUtil.writeToFile(fileToChange, "Hello2");
        GitExecutor.addCommit("my second message");

        commitHistory = GitHistoryUtils.history(firstRepo.getProject(), firstRepo.getRoot());

        var myCommit = commitHistory.get(0);
        var changes = (ArrayList) myCommit.getChanges();
        var change = (Change) changes.get(0);

        remoteRepoPath1 = Files.createTempDirectory(REMOTE_NAME_1).toRealPath(LinkOption.NOFOLLOW_LINKS).toString();

        /* set rad path */
        radicleProjectSettingsHandler = new RadicleProjectSettingsHandler(getProject());
        radicleProjectSettingsHandler.loadSettings();
        radicleProjectSettingsHandler.savePath(RAD_PATH);


        /* initialize rad stub service */
        radStub = RadStub.replaceRadicleProjectService(this, change.getBeforeRevision().getRevisionNumber().asString(), getProject());
        logger.debug("Before revision hash : {}", change.getBeforeRevision().getRevisionNumber().asString());
        logger.debug("Current revision hash : {}", firstRepo.getCurrentRevision());

        /* add seed node in config */
        initializeProject(firstRepo);
        addSeedNodeInConfig(firstRepo);
        applicationMbc = ApplicationManager.getApplication().getMessageBus().connect();
        mbc = getProject().getMessageBus().connect();
        mbc.setDefaultHandler((event1, params) -> {
            assertThat(params).hasSize(1);
            assertThat(params[0]).isInstanceOf(Notification.class);
            Notification n = (Notification) params[0];
            logger.warn("captured project notification: " + n);
            notificationsQueue.add(n);
        });
        applicationMbc.setDefaultHandler((event1, params) -> {
            assertThat(params).hasSize(1);
            assertThat(params[0]).isInstanceOf(Notification.class);
            Notification n = (Notification) params[0];
            logger.warn("captured application notification: " + n);
            notificationsQueue.add(n);
            logger.warn("notifications queue: " + notificationsQueue);
        });
        applicationMbc.subscribe(Notifications.TOPIC);
        mbc.subscribe(Notifications.TOPIC);
        logger.warn("created message bus connection and subscribed to notifications: " + mbc);
    }

    @After
    public final void after() {
        if (mbc != null) {
            mbc.disconnect();
        }
        if (applicationMbc != null) {
            applicationMbc.disconnect();
        }
        try {
            firstRepo.dispose();
            if (secondRepo != null) {
                secondRepo.dispose();
            }
        } catch (Exception e) {
            logger.warn("error disposing git repo", e);
        }
    }

    public static void assertCmd(GeneralCommandLine cmd) {
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(WSL);
            assertThat("bash").isEqualTo(cmd.getParametersList().get(0));
            assertThat("-ic").isEqualTo(cmd.getParametersList().get(1));
            assertThat(cmd.getParametersList().get(2)).contains(RAD_PATH);
        } else {
            assertThat(cmd.getCommandLineString()).contains(RAD_PATH);
        }
    }

    protected void removeRemoteRadUrl(GitRepository repo) {
        try {
            GitConfigUtil.setValue(super.getProject(), repo.getRoot(), "remote.rad.url", "");
        } catch (Exception e) {
            logger.warn("unable to write remote rad url in config file");
        }
    }

    protected void initializeProject(GitRepository repo) {
        try {
            GitConfigUtil.setValue(super.getProject(), repo.getRoot(), "remote.rad.url", "rad://hrrkbjxncopa15doj7qobxip8fotbcemjro4o.git");
        } catch (Exception e) {
            logger.warn("unable to write remote rad url in config file");
        }
    }

    protected void addSeedNodeInConfig(GitRepository repo) {
        try {
            GitConfigUtil.setValue(super.getProject(), repo.getRoot(), "rad.seed", "https://maple.radicle.garden");
        } catch (Exception e) {
            logger.warn("unable to write seed node in config file");
        }
    }

    public RadicleProjectApi replaceApiService() {
        var client = mock(CloseableHttpClient.class);
        var api = new RadicleProjectApi(myProject, client) {
            @Override
            public Session createAuthenticatedSession(GitRepository repo) {
                return new Session("testId", "testPublicKey", "testSignature");
            }
        };
        ServiceContainerUtil.replaceService(myProject, RadicleProjectApi.class, api, this.getTestRootDisposable());
        return api;
    }

    public void executeUiTasks() {
        for (int i = 0; i < 50; i++) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            CoroutineKt.executeSomeCoroutineTasksAndDispatchAllInvocationEvents(myProject);
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            Thread.yield();
        }
    }

    public static void markAsShowing(Container parent, Component inner) {
        markAsShowing(parent);
        for (var hl : parent.getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(inner, 0, inner, parent, HierarchyEvent.SHOWING_CHANGED));
        }
    }

    public static void markAsShowing(Component c) {
        var jc = (JComponent) c;

        // set headless to false, otherwise markAsShowing will be a no-op
        var prev = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "false");
        UIUtil.markAsShowing(jc, true);
        System.setProperty("java.awt.headless", prev);

        //matching UiUtil IS_SHOWING key
        jc.putClientProperty(Key.findKeyByName("Component.isShowing"), Boolean.TRUE);
        assertThat(UIUtil.isShowing(jc, false)).isTrue();
    }

    public static class NoopContinuation<T> implements Continuation<T> {
        public static final NoopContinuation<kotlin.Unit> NOOP = new NoopContinuation<>();
        @NotNull
        @Override
        public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(@NotNull Object o) {
        }
    }

    public static class MockToolWindow extends ToolWindowHeadlessManagerImpl.MockToolWindow {
        public MockToolWindow(@NotNull Project project) {
            super(project);
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public boolean isVisible() {
            return true;
        }
    }
}
