package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot;

import com.automation.remarks.junit5.Video;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.steps.CommonSteps;
import com.intellij.remoterobot.utils.Keyboard;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.IdeaFrame;
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
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static java.awt.event.KeyEvent.VK_ESCAPE;
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

            CommonSteps steps = new CommonSteps(remoteRobot);
            steps.closeProject();

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
    void initialiseRadicleProject(final RemoteRobot remoteRobot) {
        var keyboard = new Keyboard(remoteRobot);
        var sharedSteps = new ReusableSteps(remoteRobot);
        sharedSteps.importProjectFromVCS(tmpDir);

        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofMinutes(5));

        step("Wait for project to load", () -> {
            waitFor(ofMinutes(5), () -> !idea.isDumbMode());

            var projectView = remoteRobot.find(ContainerFixture.class, byXpath("ProjectViewTree", "//div[@class='ProjectViewTree']"), ofMinutes(5));
            waitFor(ofMinutes(5), () -> projectView.hasText("radicle"));
        });

        step("initialize project", () -> {
            sharedSteps.radInitializeProject(tmpDir);
            sharedSteps.refreshFromDisk();
        });


        step("Ensure Radicle sub-menu category is visible", () -> {

            remoteRobot.find(ComponentFixture.class,
                            byXpath("//div[@accessiblename='Git' and @class='ActionMenu' and @text='Git']"),
                            ofSeconds(ReusableSteps.COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)
            ).isShowing();

            keyboard.hotKey(VK_ESCAPE);
            actionMenu(remoteRobot, "Git", "").click();
            actionMenu(remoteRobot, "Radicle", "Git").isShowing();
        });

        step("Ensure Radicle sub-menu items (fetch, pull) show", () -> {
            for (int i = 0; i < 10; i++) {
                try {
                    keyboard.escape(Duration.ofSeconds(5));
                    actionMenu(remoteRobot, "Git", "").click();
                    actionMenu(remoteRobot, "Radicle", "Git").click();
                    actionMenuItem(remoteRobot, "Sync Fetch").isShowing();
                    actionMenuItem(remoteRobot, "Sync").isShowing();
                    actionMenuItem(remoteRobot, "Clone").isShowing();
                    actionMenuItem(remoteRobot, "Track").isShowing();
                    break;
                } catch (Exception ignored) {
                }
            }
            actionMenuItem(remoteRobot, "Sync Fetch").isShowing();
            actionMenuItem(remoteRobot, "Sync").isShowing();
            actionMenuItem(remoteRobot, "Clone").isShowing();
            actionMenuItem(remoteRobot, "Track").isShowing();
            Assertions.assertNull(actionMenuItem(remoteRobot, "Share Project on Radicle"));
        });

        step("Ensure Radicle toolbar actions show", () -> {
            keyboard.hotKey(VK_ESCAPE);
            isXPathComponentVisible(idea, "//div[@myicon='rad_sync.svg']");
            isXPathComponentVisible(idea, "//div[@myicon='rad_fetch.svg']");
            isXPathComponentVisible(idea, "//div[@myicon='rad_clone.svg']");
            isXPathComponentVisible(idea, "//div[@myicon='rad_pull.svg']");
        });
    }

    private void isXPathComponentVisible(IdeaFrame idea, String xpath) {
        final Locator locator = byXpath(xpath);
        idea.find(ComponentFixture.class, locator).isShowing();
    }
}
