// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.automation.remarks.junit5.Video;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.utils.Keyboard;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.IdeaFrame;
import network.radicle.jetbrains.radiclejetbrainsplugin.steps.CommonSteps;
import network.radicle.jetbrains.radiclejetbrainsplugin.utils.RemoteRobotExtension;
import network.radicle.jetbrains.radiclejetbrainsplugin.utils.StepsLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

    private final RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
    private final CommonSteps sharedSteps = new CommonSteps(remoteRobot);
    private final Keyboard keyboard = new Keyboard(remoteRobot);

    @BeforeAll
    public static void initLogging() {
        StepsLogger.init();
    }

    @AfterEach
    public void closeProject(final RemoteRobot remoteRobot) {
        step("Close the project", () -> {
            if (remoteRobot.isMac()) {
                keyboard.hotKey(VK_SHIFT, VK_META, VK_A);
                keyboard.enterText("Close Project");
                keyboard.enter();
            } else {
                actionMenu(remoteRobot, "File").click();
                actionMenuItem(remoteRobot, "Close Project").click();
            }
        });
    }

    @Test
    @Video
    void initialiseRadicleProject(final RemoteRobot remoteRobot) {
        sharedSteps.importProjectFromVCS();
        sharedSteps.closeTipOfTheDay();

        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());

        step("Ensure Radicle sub-menu category is visible", () -> {
            actionMenu(remoteRobot, "Git").click();
            actionMenu(remoteRobot, "Radicle").isShowing();
        });

        step("Ensure Radicle sub-menu items (sync, push, init) show", () -> {
            //TODO: implement
        });

        step("Ensure Radicle toolbar actions show", () -> {
            //TODO: implement
        });

//        step("Check console output", () -> {
//            final Locator locator = byXpath("//div[@class='ConsoleViewImpl']");
//            waitFor(ofMinutes(1), () -> idea.findAll(ContainerFixture.class, locator).size() > 0);
//            waitFor(ofMinutes(1), () -> idea.find(ComponentFixture.class, locator)
//                    .hasText("Hello from UI test"));
//        });
    }
}
