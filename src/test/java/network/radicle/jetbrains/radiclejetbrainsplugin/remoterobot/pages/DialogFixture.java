package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.search.locators.Locators;

@FixtureName(name = "Dialog")
public class DialogFixture extends CommonContainerFixture {
    public DialogFixture(RemoteRobot remoteRobot, RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public String getTitle() {
        return callJs("component.getTitle();");
    }

    public static Locator byTitle(String title) {
        return Locators.byXpath("title " + title, "//div[@title='" + title + "' and @class='MyDialog']");
    }
}
