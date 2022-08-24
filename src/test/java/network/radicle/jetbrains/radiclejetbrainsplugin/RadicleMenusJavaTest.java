package network.radicle.jetbrains.radiclejetbrainsplugin;

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
import java.util.Comparator;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static java.awt.event.KeyEvent.*;
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
                tmpDir = Files.createTempDirectory("test-project-" + System.currentTimeMillis());
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
    void initialiseRadicleProject(final RemoteRobot remoteRobot) throws InterruptedException {
        var keyboard = new Keyboard(remoteRobot);
        var sharedSteps = new CommonSteps(remoteRobot);
        sharedSteps.importProjectFromVCS(tmpDir);
//        sharedSteps.closeTipOfTheDay();

        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());

        step("Ensure Radicle toolbar actions show", () -> {
            keyboard.hotKey(VK_ESCAPE);
            isXPathComponentVisible(idea, "//div[@myicon='rad_pull.svg']");
            isXPathComponentVisible(idea, "//div[@myicon='rad_push.svg']");
            isXPathComponentVisible(idea, "//div[@myicon='rad_sync.svg']");
        });

        step("Ensure Radicle sub-menu category is visible", () -> {
            actionMenu(remoteRobot, "Git").click();
            actionMenu(remoteRobot, "Radicle").click();
            actionMenuItem(remoteRobot, "Pull").isShowing();
            actionMenuItem(remoteRobot, "Push").isShowing();
            actionMenuItem(remoteRobot, "Synchronize").isShowing();
            keyboard.hotKey(VK_ESCAPE);
        });

//        step("Check console output", () -> {
//            final Locator locator = byXpath("//div[@class='ConsoleViewImpl']");
//            waitFor(ofMinutes(1), () -> idea.findAll(ContainerFixture.class, locator).size() > 0);
//            waitFor(ofMinutes(1), () -> idea.find(ComponentFixture.class, locator)
//                    .hasText("Hello from UI test"));
//        });
    }

    private void isXPathComponentVisible(IdeaFrame idea, String xpath) {
        final Locator locator = byXpath(xpath);
        idea.find(ComponentFixture.class, locator).isShowing();
    }
}
