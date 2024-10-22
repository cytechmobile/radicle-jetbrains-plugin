package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.intellij.remoterobot.utils.RepeatUtilsKt;
import org.assertj.core.util.Strings;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

@FixtureName(name = "ActionMenu")
public class ActionMenuFixture extends ComponentFixture {
    public ActionMenuFixture(RemoteRobot remoteRobot, RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public static ActionMenuItemFixture actionMenu(RemoteRobot rr, String menuText, String parentMenuText) {
        final String path = Strings.isNullOrEmpty(parentMenuText) ? "//div[@class='ActionMenu' and @text='" + menuText + "']" :
                "//div[@class='ActionMenu' and @text='" + parentMenuText + "']//div[@text='" + menuText + "']";
        var xpath = byXpath("text '" + menuText + "'", path);
        RepeatUtilsKt.waitFor(() -> !rr.findAll(ActionMenuItemFixture.class, xpath).isEmpty());
        return rr.findAll(ActionMenuItemFixture.class, xpath).get(0);
    }
}
