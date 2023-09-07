package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.ui.components.panels.ListLayout;
import net.miginfocom.layout.CC;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class Utils {
    private static final String SPLIT_CHAR = ":";
    public static JComponent getVerticalPanel(int gap) {
        return new JPanel(ListLayout.vertical(gap, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
    }

    public static void addListPanel(JPanel panel, LabeledListPanelHandle<?> handle) {
        panel.add(handle.getTitleLabel(), new CC().alignY("top").width("30"));
        panel.add(handle.getPanel(), new CC().minWidth("0").growX().pushX().wrap());
    }

    public static JComponent getHorizontalPanel(int gap) {
        return new JPanel(ListLayout.horizontal(gap, ListLayout.Alignment.START, ListLayout.GrowPolicy.GROW));
    }

    public static String formatDid(String did) {
        var didStr = "did:key:";
        var parts = did.split(SPLIT_CHAR);
        if (parts.length != 3) {
            return did;
        }
        var id = parts[2];
        if (id.length() < 6) {
            return id;
        }
        var firstPart = id.substring(0, 6);
        var secondPart = id.substring(id.length() - 6);
        return didStr + firstPart + "..." + secondPart;
    }
}
