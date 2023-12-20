// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package network.radicle.jetbrains.radiclejetbrainsplugin.steps;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.JButtonFixture;
import com.intellij.remoterobot.fixtures.JLabelFixture;
import com.intellij.remoterobot.fixtures.JTreeFixture;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.DialogFixture;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.IdeaFrame;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.WelcomeFrameFixture;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import static com.intellij.remoterobot.fixtures.dataExtractor.TextDataPredicatesKt.contains;
import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static com.intellij.remoterobot.utils.UtilsKt.hasSingleComponent;
import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_COMMA;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_META;
import static java.awt.event.KeyEvent.VK_S;
import static java.time.Duration.ofSeconds;
import static network.radicle.jetbrains.radiclejetbrainsplugin.pages.DialogFixture.byTitle;

public class ReusableSteps {
    private final RemoteRobot remoteRobot;
    private final Keyboard keyboard;
    private static final int COMPONENT_SEARCH_TIMEOUT_IN_SECONDS = 60;

    public ReusableSteps(RemoteRobot remoteRobot) {

        this.remoteRobot = remoteRobot;
        keyboard = new Keyboard(remoteRobot);
    }

    public static void takeScreenshot(RemoteRobot remoteRobot, String image) {
        try {
            ImageIO.write(remoteRobot.getScreenshot(), "png", new File("build/reports", image));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void unlockIdentityWithPassphrase(RemoteRobot remoteRobot) {
        try {
            var unlockIdentityDialog = remoteRobot.find(
                    DialogFixture.class,
                    byXpath("//div[@class='MyDialog' and @title='Unlock Identity']"),
                    ofSeconds(10)
            );
            if (unlockIdentityDialog.isShowing()) {
                unlockIdentityDialog.button("OK").click();
            }
        } catch (WaitForConditionTimeoutException ignored) { }
    }

    public void importProjectFromVCS(Path localDir) {
        step("Import Project from VCS", () -> {
            final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(50));
            welcomeFrame.find(JLabelFixture.class, byXpath("//div[@text='IntelliJ IDEA']")).click();
            welcomeFrame.importProjectLink().click();

            ReusableSteps.takeScreenshot(remoteRobot, "00_import_project.png");

            final var importProjectDialog = welcomeFrame.find(DialogFixture.class, byXpath("//*[@title.key='get.from.version.control']"),
                    Duration.ofSeconds(50));
            final var urlInputFieldLocator = byXpath("//div[@class='TextFieldWithHistory']");
            remoteRobot.find(ComponentFixture.class, urlInputFieldLocator, Duration.ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)).click();
            keyboard.enterText("https://github.com/radicle-dev/radicle-cli", 0);

            final var dirInputFieldLocator = byXpath("//div[@class='TextFieldWithBrowseButton']");
            remoteRobot.find(ComponentFixture.class, dirInputFieldLocator, Duration.ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)).click();
            //create tmp dir to clone project to:
            keyboard.selectAll();
            keyboard.backspace();
            keyboard.enterText(localDir.toAbsolutePath().toString(), 0);

            ReusableSteps.takeScreenshot(remoteRobot, "01_clone.png");

            importProjectDialog.button("Clone").click();
        });
    }

    public void importRadicleProject(Path localDir) {

        step("Import Project from Radicle", () -> {

            // get from VCS
            final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(50));
            welcomeFrame.find(JLabelFixture.class, byXpath("//div[@text='IntelliJ IDEA']")).click();
            welcomeFrame.openProjectLink().click();

            final var projectPathFieldLocator = byXpath("//div[@class='BorderlessTextField']");
            remoteRobot.find(ComponentFixture.class, projectPathFieldLocator, Duration.ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)).click();
            keyboard.selectAll();
            keyboard.backspace();
            keyboard.enterText("/" + localDir.toAbsolutePath(), 0);
            keyboard.enter();

        });
    }

    public void openRadicleToolWindow() {

        step("Open Radicle Tool Window", () -> {

            final var radicleToolWindow = byXpath("//div[@accessiblename='Left Stripe']/div[@text='Radicle']");
            remoteRobot.find(ComponentFixture.class, radicleToolWindow, ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)).click();

            ReusableSteps.takeScreenshot(remoteRobot, "radicle_tool_window.png");

        });

    }

    public void configureRadicleSettings() {

        step("Open Settings", () -> {

            //shortcut to open settings
            if (SystemInfo.isWindows || SystemInfo.isLinux) {
                keyboard.hotKey(VK_CONTROL, VK_ALT, VK_S);
            } else {
                keyboard.hotKey(VK_META, VK_COMMA);
            }
            ReusableSteps.takeScreenshot(remoteRobot, "settings.png");

        });

        clickOnVersionControlSetting(remoteRobot);

        clickOnRadicleSetting(remoteRobot);

        unlockIdentity(remoteRobot, keyboard);

        applyAndSaveSettings(remoteRobot);

    }

    public void switchToRadicleIssues() {
        step("Open Radicle Issues", () -> {

            final var issuesTab = byXpath("//div[@text.key='issues open.in.browser.group.issues' and @text='Issues']");
            remoteRobot.find(ComponentFixture.class, issuesTab, Duration.ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)).click();
            remoteRobot.find(ComponentFixture.class, byXpath("//div[@myicon='refresh.svg']"), Duration.ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)).click();

        });

    }


    private static void applyAndSaveSettings(RemoteRobot remoteRobot) {
        step("Apply and Save Settings", () -> {
            var settingsDialog = remoteRobot.find(
                    DialogFixture.class,
                    byXpath("//div[@class='MyDialog' and @title='Settings']")
            );
            settingsDialog.button("OK").click();
        });
    }

    private static void unlockIdentity(RemoteRobot remoteRobot, Keyboard keyboard) {
        step("set RAD HOME", () -> {
            remoteRobot.find(
                    JButtonFixture.class,
                    byXpath("//div[@class='JLabel' and @text='Path to Profile Storage (RAD_HOME)']/following-sibling::div[@class='TextFieldWithBrowseButton']"),
                    ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)
                )
                .click();
            keyboard.selectAll();
            keyboard.backspace();
            keyboard.enterText(System.getenv("RAD_HOME"));
            ReusableSteps.takeScreenshot(remoteRobot, "rad_home.png");

        });

        step("set path to RAD CLI", () -> {
            remoteRobot.find(
                    JButtonFixture.class,
                    byXpath("//div[@class='JLabel' and @text='Path to Rad executable:']/following-sibling::div[@class='TextFieldWithBrowseButton']"),
                    ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)
                )
                .click();
            keyboard.selectAll();
            keyboard.backspace();
            keyboard.enterText(System.getenv("RAD_PATH"));
            ReusableSteps.takeScreenshot(remoteRobot, "rad_path.png");

            remoteRobot.find(
                JButtonFixture.class,
                byXpath("//div[@class='JLabel' and @text='Path to Rad executable:']/following-sibling::div[@class='JButton' and @text='Test']"),
                ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)
            )
            .click();
            ReusableSteps.takeScreenshot(remoteRobot, "rad_path_test.png");

        });


        step("Unlock Identity", () -> {
            remoteRobot.find(
                    JButtonFixture.class,
                    byXpath("//div[@class='JLabel' and @text='Path to Profile Storage (RAD_HOME)']/following-sibling::div[@class='JButton' and @text='Test']"),
                    ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)
                )
                .click();
            ReusableSteps.takeScreenshot(remoteRobot, "unlock1.png");


            unlockIdentityWithPassphrase(remoteRobot);
            ReusableSteps.takeScreenshot(remoteRobot, "unlock2.png");

        });
    }

    private static void clickOnRadicleSetting(RemoteRobot remoteRobot) {
        step("Click on Radicle Setting", () -> {
            remoteRobot.find(
                    JTreeFixture.class,
                    byXpath("//div[@class='SettingsTreeView']//div[contains(@class, 'Tree')]"),
                    ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)
                )
                .findText("Radicle")
                .doubleClick();
        });
    }

    private static void clickOnVersionControlSetting(RemoteRobot remoteRobot) {
        step("Click on Version Control Setting", () -> {
            remoteRobot.find(
                    JTreeFixture.class,
                    byXpath("//div[@class='SettingsTreeView']//div[contains(@class, 'Tree')]"),
                    ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS)
            ).findText("Version Control")
            .doubleClick();
            ReusableSteps.takeScreenshot(remoteRobot, "Version_Control.png");
        });
    }

    public void closeTipOfTheDay() {
        step("Close Tip of the Day if it appears", () -> {
            waitFor(Duration.ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS), () -> remoteRobot.findAll(
                    DialogFixture.class,
                    byXpath("//div[@class='MyDialog'][.//div[@text='Running startup activities...']]")
            ).isEmpty());
            final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
            idea.dumbAware(() -> {
                try {
                    idea.find(DialogFixture.class, byTitle("Tip of the Day")).button("Close").click();
                } catch (Throwable ignore) {
                }
                return Unit.INSTANCE;
            });
        });
    }

    public void autocomplete(String text) {
        step("Autocomplete '" + text + "'", () -> {
            final Locator completionMenu = byXpath("//div[@class='HeavyWeightWindow']");
            keyboard.enterText(text);
            waitFor(ofSeconds(5), () -> hasSingleComponent(remoteRobot, completionMenu));
            remoteRobot.find(ComponentFixture.class, completionMenu)
                    .findText(contains(text))
                    .click();
            keyboard.enter();
        });
    }
}
