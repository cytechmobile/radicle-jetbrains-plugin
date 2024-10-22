package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.intellij.remoterobot.utils.RepeatUtilsKt;

import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

@FixtureName(name = "ActionMenuItem")
public class ActionMenuItemFixture extends ComponentFixture {
    public ActionMenuItemFixture(RemoteRobot remoteRobot, RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public static ActionMenuFixture actionMenuItem(RemoteRobot rr, String text) {
        var xpath = byXpath("text '" + text + "'", "//div[@class='ActionMenuItem' and @text='" + text + "']");
        try {
            RepeatUtilsKt.waitFor(Duration.ofSeconds(5), () -> !rr.findAll(ActionMenuItemFixture.class, xpath).isEmpty());
            return rr.findAll(ActionMenuFixture.class, xpath).get(0);
        } catch (Exception e) {
            return null;
        }
    }
}
