// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package network.radicle.jetbrains.radiclejetbrainsplugin.steps;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.utils.Keyboard;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.DialogFixture;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.IdeaFrame;
import network.radicle.jetbrains.radiclejetbrainsplugin.pages.WelcomeFrameFixture;

import java.time.Duration;

import static com.intellij.remoterobot.fixtures.dataExtractor.TextDataPredicatesKt.contains;
import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static com.intellij.remoterobot.utils.UtilsKt.hasSingleComponent;
import static java.time.Duration.ofSeconds;
import static network.radicle.jetbrains.radiclejetbrainsplugin.pages.DialogFixture.byTitle;

public class CommonSteps {
    final private RemoteRobot remoteRobot;
    final private Keyboard keyboard;

    public CommonSteps(RemoteRobot remoteRobot) {
        this.remoteRobot = remoteRobot;
        this.keyboard = new Keyboard(remoteRobot);
    }

    public void importProjectFromVCS() {
        step("Import Project from VCS", () -> {
            final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(10));
            welcomeFrame.importProjectLink().click();

            final DialogFixture importProjectDialog = welcomeFrame.find(DialogFixture.class, byTitle("Get from Version Control"), Duration.ofSeconds(20));
            final Locator urlInputFieldLocator = byXpath("//div[@class='TextFieldWithHistory']");
            remoteRobot.find(ComponentFixture.class, urlInputFieldLocator, Duration.ofSeconds(20)).click();
            final Keyboard keyboard = new Keyboard(remoteRobot);
            keyboard.enterText("https://github.com/radicle-dev/radicle-cli");

            importProjectDialog.button("Clone").click();
        });
    }

    public void closeTipOfTheDay() {
        step("Close Tip of the Day if it appears", () -> {
            waitFor(Duration.ofSeconds(20), () -> remoteRobot.findAll(DialogFixture.class, byXpath("//div[@class='MyDialog'][.//div[@text='Running startup activities...']]")).size() == 0);
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
            final Keyboard keyboard = new Keyboard(remoteRobot);
            keyboard.enterText(text);
            waitFor(ofSeconds(5), () -> hasSingleComponent(remoteRobot, completionMenu));
            remoteRobot.find(ComponentFixture.class, completionMenu)
                    .findText(contains(text))
                    .click();
            keyboard.enter();
        });
    }
}