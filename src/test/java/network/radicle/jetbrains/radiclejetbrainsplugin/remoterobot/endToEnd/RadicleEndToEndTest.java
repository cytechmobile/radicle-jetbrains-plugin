package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.endToEnd;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.utils.Keyboard;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.IdeaFrame;
import network.radicle.jetbrains.radiclejetbrainsplugin.steps.CommonSteps;
import network.radicle.jetbrains.radiclejetbrainsplugin.utils.RemoteRobotExtension;
import network.radicle.jetbrains.radiclejetbrainsplugin.utils.StepsLogger;
import org.junit.jupiter.api.*;
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
import static java.awt.event.KeyEvent.*;
import static network.radicle.jetbrains.radiclejetbrainsplugin.pages.ActionMenuFixtureKt.actionMenu;
import static network.radicle.jetbrains.radiclejetbrainsplugin.pages.ActionMenuFixtureKt.actionMenuItem;

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

            if (remoteRobot.isMac()) {
                keyboard.hotKey(VK_SHIFT, VK_META, VK_A);
                keyboard.enterText("Close Project");
                keyboard.enter();
            } else {
                actionMenu(remoteRobot, "File").click();
                actionMenuItem(remoteRobot, "Close Project").click();
            }

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
    void radicleIssueIsShowing(final RemoteRobot remoteRobot) {
        var keyboard = new Keyboard(remoteRobot);
        var sharedSteps = new CommonSteps(remoteRobot);
        sharedSteps.closeTipOfTheDay();

        // TODO: setup plugin
        openRadicleToolWindow(remoteRobot, keyboard);
        switchToRadicleIssues();
        createRadicleIssue();
        openFirstRadicleIssue();

    }

    private void openFirstRadicleIssue() {
        // TODO: click on first issue
        // TODO: verify editor tab opens and it shows the issue title

    }

    private void createRadicleIssue() {
        // TODO: create an issue

    }

    private void switchToRadicleIssues() {
        // TODO: click on issues

    }

    private void openRadicleToolWindow(RemoteRobot remoteRobot, Keyboard keyboard) {
        // TODO:
        step("Open Radicle Tool Window", () -> {
            keyboard.hotKey(VK_ESCAPE);
            final var radicleToolWindow = byXpath("//div[contains(@text.key, 'radicle')]");
            remoteRobot.find(ComponentFixture.class, radicleToolWindow, Duration.ofSeconds(20)).click();

        });

    }

    private void isXPathComponentVisible(IdeaFrame idea, String xpath) {
        final Locator locator = byXpath(xpath);
        idea.find(ComponentFixture.class, locator).isShowing();
    }
}
