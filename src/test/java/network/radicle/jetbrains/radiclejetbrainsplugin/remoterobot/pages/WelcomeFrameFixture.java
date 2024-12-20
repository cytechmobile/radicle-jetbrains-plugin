// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.steps.ReusableSteps;
import org.jetbrains.annotations.NotNull;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.utils.UtilsKt.hasAnyComponent;


@DefaultXpath(by = "FlatWelcomeFrame type", xpath = "//div[@class='FlatWelcomeFrame']")
@FixtureName(name = "Welcome Frame")
public class WelcomeFrameFixture extends CommonContainerFixture {
    public WelcomeFrameFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public ComponentFixture createNewProjectLink() {
        return welcomeFrameLink("New Project");
    }

    public ComponentFixture importProjectLink() {
        return welcomeFrameLink("Get from VCS");
    }
    public ComponentFixture openProjectLink() {
        return welcomeFrameLink("Open");
    }

    private ComponentFixture welcomeFrameLink(String text) {
        if (hasAnyComponent(this, byXpath("//div[@class='JBScrollPane']"))) {
            return find(ComponentFixture.class, byXpath("//div[@class='JBOptionButton' and @text='" + text + "']"),
                    ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION);
        }
        return find(ComponentFixture.class,
                byXpath("//div[@class='NonOpaquePanel'][./div[@text='" + text + "']]//div[@class='JButton']"), ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION);
    }
}
