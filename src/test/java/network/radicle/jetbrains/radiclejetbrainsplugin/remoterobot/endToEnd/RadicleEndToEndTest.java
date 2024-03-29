package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.endToEnd;

import com.automation.remarks.junit5.Video;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.JLabelFixture;
import com.intellij.remoterobot.fixtures.JTextAreaFixture;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.steps.CommonSteps;
import com.intellij.remoterobot.utils.Keyboard;
import com.jgoodies.common.base.Strings;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.IdeaFrame;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.WelcomeFrameFixture;
import network.radicle.jetbrains.radiclejetbrainsplugin.steps.ReusableSteps;
import network.radicle.jetbrains.radiclejetbrainsplugin.utils.RemoteRobotExtension;
import network.radicle.jetbrains.radiclejetbrainsplugin.utils.StepsLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.time.Duration.ofSeconds;

@ExtendWith(RemoteRobotExtension.class)
@Tag("UI")
public class RadicleEndToEndTest {
    private static final Logger logger = LoggerFactory.getLogger(RadicleEndToEndTest.class);
    Path tmpDir;

    @BeforeAll
    public static void initLogging() {
        StepsLogger.init();
    }

    @BeforeEach
    public void beforeEach() {
        step("Create tmp dir", () -> {
            try {
                tmpDir = Files.createTempDirectory("test-project");
            } catch (Exception e) {
                logger.warn("error creating temp directory", e);
                Assertions.fail("error creating temp directory", e);
            }
        });
    }

    @AfterEach
    public void closeProject(final RemoteRobot remoteRobot) {
        step("Close the project", () -> {
            var keyboard = new Keyboard(remoteRobot);
            // close any possibly open menu
            keyboard.hotKey(VK_ESCAPE);

            var commonSteps = new CommonSteps(remoteRobot);
            commonSteps.closeProject();

            logger.warn("deleting tmp dir: {}", tmpDir.toFile());
            try {
                // FileUtils.deleteDirectory(tmpDir.toFile());
                Files.walk(tmpDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (Exception e) {
                logger.warn("error deleting directory", e);
            }
        });
    }

    @Test
    @Tag("video")
    @Video
    void createAndDisplayRadicleIssue(final RemoteRobot remoteRobot) {
        var sharedSteps = new ReusableSteps(remoteRobot);
//        sharedSteps.closeTipOfTheDay();

        final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(50));
        welcomeFrame.find(JLabelFixture.class, byXpath("//div[@text='IntelliJ IDEA']")).click();

        openProject(remoteRobot);

        sharedSteps.configureRadicleSettings();
        sharedSteps.openRadicleToolWindow();
        sharedSteps.switchToRadicleIssues();

        var time = System.currentTimeMillis();
        final String issueTitle = "Automated issue [%d]".formatted(time);
        createRadicleIssue(remoteRobot, issueTitle);
        openFirstRadicleIssue(remoteRobot, issueTitle);

    }

    private static void openProject(RemoteRobot remoteRobot) {
        CommonSteps commonSteps = new CommonSteps(remoteRobot);

        final String projectPath = System.getenv("PROJECT_PATH");
        final String radicleRepo = System.getenv("RADICLE_REPO");

        if (Strings.isNotEmpty(projectPath)) {
            commonSteps.openProject(projectPath);
        } else if (Strings.isNotEmpty(radicleRepo)) {
            commonSteps.openProject(radicleRepo);
        }

        commonSteps.waitForSmartMode(5);

    }

    private void openFirstRadicleIssue(RemoteRobot remoteRobot, String issueTitle) {
        remoteRobot.find(
                ComponentFixture.class,
                byXpath("//div[@class='JBList']"),
                Duration.ofSeconds(20)
        )
        .findText(issueTitle)
        .doubleClick();

        // assert new editor tab has opened
        remoteRobot.find(
                JLabelFixture.class,
                byXpath("//div[@class='SimpleColoredComponent' and contains(@visible_text, '" + issueTitle + "')]"),
                ofSeconds(20)
        ).isShowing();

    }

    private void createRadicleIssue(RemoteRobot remoteRobot, String issueTitle) {
        step("Create Radicle Issue", () -> {
            final var keyboard = new Keyboard(remoteRobot);

            final var issuesTab = byXpath("//div[@class='ToolWindowHeader'][.//div[@text='Issues']]//div[@myicon='add.svg']");
            remoteRobot.find(ComponentFixture.class, issuesTab, Duration.ofSeconds(20)).click();

            final var issueTitleInput = byXpath("//div[@class='JBTextArea' and @visible_text='Title']");
            remoteRobot.find(JTextAreaFixture.class, issueTitleInput, Duration.ofSeconds(20)).click();

            keyboard.enterText(issueTitle, 0);

            final var issueDescriptionInput = byXpath("//div[@class='DragAndDropField' and @visible_text='Description']");
            remoteRobot.find(JTextAreaFixture.class, issueDescriptionInput, Duration.ofSeconds(20)).click();

            keyboard.enterText("Automated issue. Please ignore", 0);
            keyboard.enter();

            final var createButton = byXpath("//div[@class='JButton' and @visible_text='Create Issue']");
            remoteRobot.find(JTextAreaFixture.class, createButton, Duration.ofSeconds(20)).click();

            ReusableSteps.unlockIdentityWithPassphrase(remoteRobot);

        });

    }


    private void isXPathComponentVisible(IdeaFrame idea, String xpath) {
        final Locator locator = byXpath(xpath);
        idea.find(ComponentFixture.class, locator).isShowing();
    }
}
