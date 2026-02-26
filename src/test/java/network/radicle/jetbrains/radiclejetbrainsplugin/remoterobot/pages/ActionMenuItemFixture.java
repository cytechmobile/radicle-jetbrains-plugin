package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.utils.RepeatUtilsKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

@FixtureName(name = "ActionMenuItem")
public class ActionMenuItemFixture extends ComponentFixture {
    private static final Logger logger = LoggerFactory.getLogger(ActionMenuItemFixture.class);
    public ActionMenuItemFixture(RemoteRobot remoteRobot, RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public static ActionMenuItemFixture actionMenuItem(RemoteRobot rr, String text) {
        try {
            RepeatUtilsKt.waitFor(Duration.ofSeconds(5), () -> !findActionMenuItems(rr, text).isEmpty());
            return findActionMenuItems(rr, text).getFirst();
        } catch (Exception e) {
            logger.warn("Error trying to find action menu item with text:" + text, e);
            return null;
        }
    }

    public static Locator getActionMenuItemLocator(String text) {
        return byXpath("text '" + text + "'", "//div[@class='ActionMenuItem' and @text='" + text + "']");
    }

    public static List<ActionMenuItemFixture> findActionMenuItems(RemoteRobot rr, String text) {
        return rr.findAll(ActionMenuItemFixture.class, getActionMenuItemLocator(text));
    }
}
