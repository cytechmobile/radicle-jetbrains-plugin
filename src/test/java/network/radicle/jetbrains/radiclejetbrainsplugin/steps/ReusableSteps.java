// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package network.radicle.jetbrains.radiclejetbrainsplugin.steps;

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

import java.nio.file.Path;
import java.time.Duration;

import static com.intellij.remoterobot.fixtures.dataExtractor.TextDataPredicatesKt.contains;
import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static com.intellij.remoterobot.utils.UtilsKt.hasSingleComponent;
import static java.awt.event.KeyEvent.VK_COMMA;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_META;
import static java.time.Duration.ofSeconds;
import static network.radicle.jetbrains.radiclejetbrainsplugin.pages.DialogFixture.byTitle;

public class ReusableSteps {
    private final RemoteRobot remoteRobot;
    private final Keyboard keyboard;

    public ReusableSteps(RemoteRobot remoteRobot) {

        this.remoteRobot = remoteRobot;
        keyboard = new Keyboard(remoteRobot);
    }

    public void importProjectFromVCS(Path localDir) {
        step("Import Project from VCS", () -> {
            final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(50));
            welcomeFrame.find(JLabelFixture.class, byXpath("//div[@text='IntelliJ IDEA']")).click();
            welcomeFrame.importProjectLink().click();

            final var importProjectDialog = welcomeFrame.find(DialogFixture.class, byXpath("//*[@title.key='get.from.version.control']"),
                    Duration.ofSeconds(50));
            final var urlInputFieldLocator = byXpath("//div[@class='TextFieldWithHistory']");
            remoteRobot.find(ComponentFixture.class, urlInputFieldLocator, Duration.ofSeconds(20)).click();
            keyboard.enterText("https://github.com/radicle-dev/radicle-cli", 0);

            final var dirInputFieldLocator = byXpath("//div[@class='TextFieldWithBrowseButton']");
            remoteRobot.find(ComponentFixture.class, dirInputFieldLocator, Duration.ofSeconds(20)).click();
            //create tmp dir to clone project to:
            keyboard.selectAll();
            keyboard.backspace();
            keyboard.enterText(localDir.toAbsolutePath().toString(), 0);

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
            remoteRobot.find(ComponentFixture.class, projectPathFieldLocator, Duration.ofSeconds(20)).click();
            keyboard.selectAll();
            keyboard.backspace();
            keyboard.enterText("/" + localDir.toAbsolutePath(), 0);
            keyboard.enter();

        });
    }

    public void openRadicleToolWindow() {

        step("Open Radicle Tool Window", () -> {
            keyboard.hotKey(VK_ESCAPE);

            try {
                remoteRobot.find(JLabelFixture.class, byXpath("//div[@text='Patches']"), ofSeconds(10));
            } catch (WaitForConditionTimeoutException timeout) {
                final var radicleToolWindow = byXpath("//div[@text='Radicle']");
                remoteRobot.find(ComponentFixture.class, radicleToolWindow, ofSeconds(20)).click();
            }

        });

    }

    public void configureRadicleSettings() {

        step("Configure Radicle Settings", () -> {

            //shortcut to open settings
            keyboard.hotKey(VK_META, VK_COMMA);

            clickOnVersionControlSetting(remoteRobot);

            clickOnRadicleSetting(remoteRobot);

            unlockIdentity(remoteRobot);

            applyAndSaveSettings(remoteRobot);

        });

    }

    public void switchToRadicleIssues() {
        step("Open Radicle Issues", () -> {

            final var issuesTab = byXpath("//div[@text.key='issues open.in.browser.group.issues' and @text='Issues']");
            remoteRobot.find(ComponentFixture.class, issuesTab, Duration.ofSeconds(20)).click();

        });

    }


    private static void applyAndSaveSettings(RemoteRobot remoteRobot) {
        var settingsDialog = remoteRobot.find(
                DialogFixture.class,
                byXpath("//div[@class='MyDialog' and @title='Settings']")
        );
        settingsDialog.button("OK").click();
    }

    private static void unlockIdentity(RemoteRobot remoteRobot) {
        remoteRobot.find(
                JButtonFixture.class,
                byXpath("//div[@class='JLabel' and @text='Path to Profile Storage (RAD_HOME)']/following-sibling::div[@class='JButton' and @text='Test']"),
                ofSeconds(20)
            )
            .click();

        var unlockIdentityDialog = remoteRobot.find(
                DialogFixture.class,
                byXpath("//div[@class='MyDialog' and @title='Unlock Identity']"),
                ofSeconds(20)
        );
        unlockIdentityDialog.button("OK").click();

    }

    private static void clickOnRadicleSetting(RemoteRobot remoteRobot) {
        remoteRobot.find(
                JTreeFixture.class,
                byXpath("//div[@class='SettingsTreeView']//div[contains(@class, 'Tree')]"),
                ofSeconds(20)
            )
            .findText("Radicle")
            .click();
    }

    private static void clickOnVersionControlSetting(RemoteRobot remoteRobot) {
        remoteRobot.find(
                JTreeFixture.class,
                byXpath("//div[@class='SettingsTreeView']//div[contains(@class, 'Tree')]"),
                ofSeconds(20)
            )
            .findText("Version Control")
            .click();
    }

    public void closeTipOfTheDay() {
        step("Close Tip of the Day if it appears", () -> {
            waitFor(Duration.ofSeconds(20), () -> remoteRobot.findAll(DialogFixture.class,
                    byXpath("//div[@class='MyDialog'][.//div[@text='Running startup activities...']]")).size() == 0);
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
