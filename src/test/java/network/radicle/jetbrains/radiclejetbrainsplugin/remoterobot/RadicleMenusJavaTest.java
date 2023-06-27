package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.utils.Keyboard;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.IdeaFrame;
import network.radicle.jetbrains.radiclejetbrainsplugin.steps.CommonSteps;
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
import java.util.Comparator;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_META;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static network.radicle.jetbrains.radiclejetbrainsplugin.pages.ActionMenuFixtureKt.actionMenu;
import static network.radicle.jetbrains.radiclejetbrainsplugin.pages.ActionMenuFixtureKt.actionMenuItem;

@ExtendWith(RemoteRobotExtension.class)
@Tag("UI")
public class RadicleMenusJavaTest {
    private static final Logger logger = LoggerFactory.getLogger(RadicleMenusJavaTest.class);
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
    void initialiseRadicleProject(final RemoteRobot remoteRobot) {
        var keyboard = new Keyboard(remoteRobot);
        var sharedSteps = new CommonSteps(remoteRobot);
        sharedSteps.importProjectFromVCS(tmpDir);
//        sharedSteps.closeTipOfTheDay();

        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());
        step("Ensure Radicle sub-menu category is visible", () -> {
            keyboard.hotKey(VK_ESCAPE);
            actionMenu(remoteRobot, "Git").click();
            actionMenu(remoteRobot, "Radicle").isShowing();
        });

        step("Ensure Radicle sub-menu items (fetch, pull) show", () -> {
            keyboard.hotKey(VK_ESCAPE);
            actionMenu(remoteRobot, "Git").click();
            actionMenu(remoteRobot, "Radicle").click();
            actionMenuItem(remoteRobot, "Sync").isShowing();
            actionMenuItem(remoteRobot, "Clone").isShowing();
            actionMenuItem(remoteRobot, "Track").isShowing();
            actionMenuItem(remoteRobot, "Share Project on Radicle").isShowing();
        });

        step("Ensure Radicle toolbar actions show", () -> {
            keyboard.hotKey(VK_ESCAPE);
            isXPathComponentVisible(idea, "//div[@myicon='rad_fetch.svg']");
            isXPathComponentVisible(idea, "//div[@myicon='rad_clone.svg']");
        });
    }

    private void isXPathComponentVisible(IdeaFrame idea, String xpath) {
        final Locator locator = byXpath(xpath);
        idea.find(ComponentFixture.class, locator).isShowing();
    }
}