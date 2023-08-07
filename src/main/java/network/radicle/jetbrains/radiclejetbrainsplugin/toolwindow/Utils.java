package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.ui.components.panels.ListLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class Utils {
    public static JComponent getVerticalPanel(int gap) {
        return new JPanel(ListLayout.vertical(gap, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
    }

    public static void addListPanel(JPanel panel, Utils.LabeledListPanelHandle<?> handle) {
        panel.add(handle.getTitleLabel(), new CC().alignY("top").width("30"));
        panel.add(handle.getPanel(), new CC().minWidth("0").growX().pushX().wrap());
    }

    public static JComponent getHorizontalPanel(int gap) {
        return new JPanel(ListLayout.horizontal(gap, ListLayout.Alignment.START, ListLayout.GrowPolicy.GROW));
    }
}
