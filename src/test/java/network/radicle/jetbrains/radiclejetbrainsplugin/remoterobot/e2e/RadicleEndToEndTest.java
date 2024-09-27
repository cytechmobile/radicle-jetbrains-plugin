package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.e2e;

import com.automation.remarks.junit5.Video;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.JLabelFixture;
import com.intellij.remoterobot.fixtures.JTextAreaFixture;
import com.intellij.remoterobot.search.locators.Locators;
import com.intellij.remoterobot.steps.CommonSteps;
import com.intellij.remoterobot.utils.Keyboard;
import network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages.WelcomeFrameFixture;
import network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.steps.ReusableSteps;
import network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.utils.RemoteRobotExtension;
import network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.utils.StepsLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;
import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(RemoteRobotExtension.class)
@Tag("UI")
public class RadicleEndToEndTest {
    private static final Logger logger = LoggerFactory.getLogger(RadicleEndToEndTest.class);

    @BeforeAll
    public static void initLogging() {
        assertThat(System.getenv("RAD_HOME")).as("No RAD_HOME env var is set").isNotEmpty();
        assertThat(System.getenv("RAD_PATH")).as("No RAD_PATH env var is set").isNotEmpty();
        assertThat(System.getenv("RADICLE_REPO")).as("No RADICLE_REPO env var is set").isNotEmpty();
        StepsLogger.init();
    }

    @AfterEach
    public void closeProject(final RemoteRobot remoteRobot) {
        step("Close the project", () -> {
            var keyboard = new Keyboard(remoteRobot);
            // close any possibly open menu
            keyboard.hotKey(KeyEvent.VK_ESCAPE);

            var commonSteps = new CommonSteps(remoteRobot);
            commonSteps.closeProject();
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
        logger.warn("going to open project");
        openProject(remoteRobot);

        sharedSteps.configureRadicleSettings();
        sharedSteps.openRadicleToolWindow();
        sharedSteps.switchToRadicleIssues();

        var time = System.currentTimeMillis();
        final String issueTitle = "Automated issue [%d]".formatted(time);
        createRadicleIssue(remoteRobot, issueTitle);
        openFirstRadicleIssue(remoteRobot, issueTitle);
    }

    private void openFirstRadicleIssue(RemoteRobot remoteRobot, String issueTitle) {
        remoteRobot.find(ComponentFixture.class, byXpath("//div[@class='JBList']"), Duration.ofSeconds(20)).findText(issueTitle).doubleClick();

        // assert new editor tab has opened
        remoteRobot.find(JLabelFixture.class, byXpath("//div[@class='SimpleColoredComponent' and contains(@visible_text, '" + issueTitle + "')]"),
                Duration.ofSeconds(20)).isShowing();
    }

    private void createRadicleIssue(RemoteRobot remoteRobot, String issueTitle) {
        step("Create Radicle Issue", () -> {
            final var keyboard = new Keyboard(remoteRobot);
            final var issuesTab = byXpath("//div[@class='ToolWindowHeader'][.//div[@text='Issues']]//div[@myicon='add.svg']");
            remoteRobot.find(ComponentFixture.class, issuesTab, ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION).click();

            final var issueTitleInput = byXpath("//div[@class='JBTextArea' and @visible_text='Title']");
            remoteRobot.find(JTextAreaFixture.class, issueTitleInput, ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION).click();

            keyboard.enterText(issueTitle, 0);

            // and @visible_text='Description'] (this is further down in the tree)
            final var issueDescriptionInput = byXpath("//div[@class='DragAndDropField']//div[@class='EditorComponentImpl' and @visible_text='Description']");
            remoteRobot.find(JTextAreaFixture.class, issueDescriptionInput, ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION).click();

            keyboard.enterText("Automated issue. Please ignore", 0);
            keyboard.enter();

            final var createButton = byXpath("//div[@class='JButton' and @visible_text='Create Issue']");
            remoteRobot.find(JTextAreaFixture.class, createButton, ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION).click();

            ReusableSteps.unlockIdentityWithPassphrase(remoteRobot);
        });
    }

    private static void openProject(RemoteRobot remoteRobot) {
        CommonSteps commonSteps = new CommonSteps(remoteRobot);

        final String radicleRepo = System.getenv("RADICLE_REPO");
        logger.warn("opening project: {}", radicleRepo);
        commonSteps.openProject(radicleRepo);

        //commonSteps.waitForSmartMode(5);
        remoteRobot.find(ComponentFixture.class, Locators.byXpath("//div[@class='EditorCompositePanel']"), ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION);
    }
}
