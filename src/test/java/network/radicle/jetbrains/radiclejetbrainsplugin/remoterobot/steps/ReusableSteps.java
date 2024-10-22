// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.steps;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.JButtonFixture;
import com.intellij.remoterobot.fixtures.JLabelFixture;
import com.intellij.remoterobot.fixtures.JTreeFixture;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages.DialogFixture;
import network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages.WelcomeFrameFixture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages.ActionMenuFixture.actionMenu;
import static network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages.ActionMenuItemFixture.actionMenuItem;
import static org.assertj.core.api.Assertions.assertThat;

public class ReusableSteps {
    private static final Logger log = LoggerFactory.getLogger(ReusableSteps.class);
    private final RemoteRobot remoteRobot;
    private final Keyboard keyboard;
    public static final int COMPONENT_SEARCH_TIMEOUT_IN_SECONDS = 60;
    public static final Duration COMPONENT_SEARCH_TIMEOUT_DURATION = Duration.ofSeconds(COMPONENT_SEARCH_TIMEOUT_IN_SECONDS);

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
            var unlockIdentityDialog = remoteRobot.find(DialogFixture.class, byXpath("//div[@class='MyDialog' and @title='Unlock Identity']"),
                    Duration.ofSeconds(10));
            if (unlockIdentityDialog.isShowing()) {
                unlockIdentityDialog.button("OK").click();
            }
        } catch (WaitForConditionTimeoutException ignored) { }
    }

    public void refreshFromDisk() {
        for (int i = 0; i < 10; i++) {
            try {
                actionMenu(remoteRobot, "File", "").click();
                var menuItem = actionMenuItem(remoteRobot, "Reload All from Disk");
                if (menuItem != null) {
                    menuItem.click();
                    break;
                }
            } catch (Exception e) {
                log.warn("error reloading all from disk, retrying", e);
            }
            waitForMs(500);
        }
    }

    public void radInitializeProject(Path localDir) {
            String path = "";
            if (SystemInfo.isWindows) {
                path = localDir.toAbsolutePath().toString().replace("\\", "\\\\") + "\\.git\\config";
            } else {
                path = localDir.toAbsolutePath() + "/.git/config";
            }
            try {
                File f = new File(path);
                if (!f.exists()) {
                    throw new RuntimeException();
                }
                FileWriter fw = new FileWriter(path, true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write("[remote \"rad\"]\n" +
                        "\turl = rad://test\n" +
                        "\tpushurl = rad://test/test\n" +
                        "\tfetch = +refs/heads/*:refs/remotes/rad/*");
                bw.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    public void importProjectFromVCS(Path localDir) {
        step("Import Project from VCS", () -> {
            final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(50));
            welcomeFrame.find(JLabelFixture.class, byXpath("//div[@text='IntelliJ IDEA']")).click();
            welcomeFrame.importProjectLink().click();

            final var urlInputFieldLocator = byXpath("//div[@class='TextFieldWithHistory']");
            remoteRobot.find(ComponentFixture.class, urlInputFieldLocator, COMPONENT_SEARCH_TIMEOUT_DURATION).click();
            keyboard.enterText("https://seed.radicle.xyz/z3gqcJUoA1n9HaHKufZs5FCSGazv5.git", 0);

            final var dirInputFieldLocator = byXpath("//div[@class='TextFieldWithBrowseButton']");
            remoteRobot.find(ComponentFixture.class, dirInputFieldLocator, COMPONENT_SEARCH_TIMEOUT_DURATION).click();
            //create tmp dir to clone project to:
            keyboard.selectAll();
            keyboard.backspace();
            if (SystemInfo.isWindows) {
                keyboard.enterText(localDir.toAbsolutePath().toString().replace("\\", "\\\\"), 0);
            } else {
                keyboard.enterText(localDir.toAbsolutePath().toString(), 0);
            }
            remoteRobot.find(JButtonFixture.class, byXpath("//div[@text='Clone']")).click();
        });
    }

    public void importRadicleProject(Path localDir) {
        step("Import Project from Radicle", () -> {
            // get from VCS
            final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(50));
            welcomeFrame.find(JLabelFixture.class, byXpath("//div[@text='IntelliJ IDEA']")).click();
            welcomeFrame.openProjectLink().click();

            final var projectPathFieldLocator = byXpath("//div[@class='BorderlessTextField']");
            remoteRobot.find(ComponentFixture.class, projectPathFieldLocator, COMPONENT_SEARCH_TIMEOUT_DURATION).click();
            keyboard.selectAll();
            keyboard.backspace();
            keyboard.enterText("/" + localDir.toAbsolutePath(), 0);
            keyboard.enter();
        });
    }

    public void openRadicleToolWindow() {
        step("Open Radicle Tool Window", () -> {
            final var radicleToolWindow = byXpath("//div[@accessiblename='Left Stripe']/div[@text='Radicle']");
            remoteRobot.find(ComponentFixture.class, radicleToolWindow, COMPONENT_SEARCH_TIMEOUT_DURATION).click();
        });
    }

    public void configureRadicleSettings() {
        clickOnVersionControlSetting(remoteRobot, keyboard);
        clickOnRadicleSetting(remoteRobot);
        unlockIdentity(remoteRobot, keyboard);
        applyAndSaveSettings(remoteRobot);
    }

    public void switchToRadicleIssues() {
        step("Open Radicle Issues", () -> {
            final var issuesTab = byXpath("//div[contains(@text.key, 'issues') and @text='Issues']");
            remoteRobot.find(ComponentFixture.class, issuesTab, COMPONENT_SEARCH_TIMEOUT_DURATION).click();
            remoteRobot.find(ComponentFixture.class, byXpath("//div[@myicon='refresh.svg']"), COMPONENT_SEARCH_TIMEOUT_DURATION).click();
        });
    }

    private static void applyAndSaveSettings(RemoteRobot remoteRobot) {
        step("Apply and Save Settings", () -> {
           // var settingsTitle = (SystemInfo.isMac) ? "Preferences" : "Settings";
            var settingsTitle = "Settings";
            var settingsDialog = remoteRobot.find(
                    DialogFixture.class,
                    byXpath("//div[@class='MyDialog' and @title='" + settingsTitle + "']"),
                    COMPONENT_SEARCH_TIMEOUT_DURATION
            );
            settingsDialog.button("OK").click();
        });
    }

    private static void unlockIdentity(RemoteRobot remoteRobot, Keyboard keyboard) {
        step("set RAD HOME", () -> {
            var profile = remoteRobot.find(JButtonFixture.class,
                    byXpath("//div[@class='JLabel' and @text='Path to Profile Storage (RAD_HOME):']" +
                            "/following-sibling::div[@class='TextFieldWithBrowseButton']"),
                    COMPONENT_SEARCH_TIMEOUT_DURATION);
            if (!profile.getText().equals(System.getenv("RAD_HOME"))) {
                profile.click();
                ReusableSteps.takeScreenshot(remoteRobot, "rad_home_click.png");
                keyboard.selectAll();
                ReusableSteps.takeScreenshot(remoteRobot, "rad_home_select.png");
                keyboard.backspace();
                ReusableSteps.takeScreenshot(remoteRobot, "rad_home_empty.png");
                keyboard.enterText(System.getenv("RAD_HOME"));
                ReusableSteps.takeScreenshot(remoteRobot, "rad_home.png");
            }
        });

        step("set path to RAD CLI", () -> {
            var radPath = remoteRobot.find(JButtonFixture.class,
                    byXpath("//div[@class='JLabel' and @text='Path to Rad executable:']/following-sibling::div[@class='TextFieldWithBrowseButton']"),
                    COMPONENT_SEARCH_TIMEOUT_DURATION);
            if (!radPath.getText().equals(System.getenv("RAD_PATH"))) {
                radPath.click();
                keyboard.selectAll();
                keyboard.backspace();
                keyboard.enterText(System.getenv("RAD_PATH"));
                ReusableSteps.takeScreenshot(remoteRobot, "rad_path.png");
            }

            remoteRobot.find(JButtonFixture.class,
                    byXpath("//div[@class='JLabel' and @text='Path to Rad executable:']/following-sibling::div[@class='JButton' and @text='Test']"),
                    COMPONENT_SEARCH_TIMEOUT_DURATION)
                    .click();
            ReusableSteps.takeScreenshot(remoteRobot, "rad_path_test.png");
        });

        step("Unlock Identity", () -> {
            remoteRobot.find(JButtonFixture.class,
                    byXpath("//div[@class='JLabel' and @text='Path to Profile Storage (RAD_HOME):']/following-sibling::div[@class='JButton' and @text='Test']"),
                    COMPONENT_SEARCH_TIMEOUT_DURATION)
                    .click();
            ReusableSteps.takeScreenshot(remoteRobot, "unlock1.png");

            unlockIdentityWithPassphrase(remoteRobot);
            ReusableSteps.takeScreenshot(remoteRobot, "unlock2.png");
        });
    }

    private static void clickOnRadicleSetting(RemoteRobot remoteRobot) {
        step("Click on Radicle Setting", () -> {
            boolean clicked = false;
            for (int i = 0; i < 20; i++) {
                try {
                    var tree = remoteRobot.find(JTreeFixture.class, byXpath("//div[@class='SettingsTreeView']//div[@class='MyTree']"),
                            COMPONENT_SEARCH_TIMEOUT_DURATION);
                    if (!tree.hasText("Radicle")) {
                        log.warn("no Radicle in Version Control");
                        waitForMs(3_000);
                        continue;
                    }
                    log.warn("double clicking Radicle in Version Control in Settings");
                    tree.findText("Radicle").doubleClick();
                    clicked = true;
                    ReusableSteps.takeScreenshot(remoteRobot, "radicle_settings.png");
                    break;
                } catch (Exception e) {
                    ReusableSteps.takeScreenshot(remoteRobot, "radicle_settings_retry.png");
                    waitForMs(3_000);
                }
            }
            assertThat(clicked).as("unable to click radicle in version control in settings").isTrue();
        });
    }

    private static void clickOnVersionControlSetting(RemoteRobot remoteRobot, Keyboard keyboard) {
        step("Open Settings", () -> {
            openSettingsWithHotKey(keyboard);
        });

        step("Click on Version Control Setting", () -> {
            boolean clicked = false;
            for (int i = 0; i < 20; i++) {
                try {
                    var tree = remoteRobot.find(JTreeFixture.class, byXpath("//div[@class='SettingsTreeView']//div[@class='MyTree']"),
                            COMPONENT_SEARCH_TIMEOUT_DURATION);
                    if (!tree.hasText("Version Control")) {
                        log.warn("no version control in tree: {}", tree);
                        waitForMs(3_000);
                        continue;
                    }
                    log.warn("double clicking version control");
                    tree.findText("Version Control").doubleClick();
                    clicked = true;
                    ReusableSteps.takeScreenshot(remoteRobot, "settings.png");
                    break;
                } catch (Exception e) {
                    log.warn("error clicking on version control setting, re-opening settings and retrying", e);
                    ReusableSteps.takeScreenshot(remoteRobot, "settings_retry.png");
                    openSettingsWithHotKey(keyboard);
                }
            }
            ReusableSteps.takeScreenshot(remoteRobot, "Version_Control.png");
            assertThat(clicked).as("unable to click Version Control in settings").isTrue();
        });
    }

    private static void openSettingsWithHotKey(Keyboard keyboard) {
        //shortcut to open settings
        for (int i = 0; i < 10; i++) {
            try {
                if (SystemInfo.isWindows || SystemInfo.isLinux) {
                    keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_S);
                } else {
                    keyboard.hotKey(KeyEvent.VK_META, KeyEvent.VK_COMMA);
                }
            } catch (Exception e) {
                log.warn("error opening settings with hotkey, retrying");
                waitForMs(5_000);
            }
        }
    }

    public static void waitForMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }
}
