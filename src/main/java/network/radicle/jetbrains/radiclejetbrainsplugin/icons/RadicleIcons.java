package network.radicle.jetbrains.radiclejetbrainsplugin.icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.registry.Registry;

import javax.swing.Icon;

public class  RadicleIcons {
    private static final String ICON_PATH = "/icons/";

    public static Icon RADICLE = getIcon("radicle", false);

    public static Icon RADICLE_TOOL_WINDOW = getIcon("rad_tool_window", true);

    public static Icon RADICLE_PUSH = getIcon("rad_push", true);

    public static Icon RADICLE_PULL = getIcon("rad_pull", true);

    public static Icon RADICLE_SYNC = getIcon("rad_sync", true);

    public static Icon RADICLE_CLONE = getIcon("rad_clone", true);

    public static Icon RADICLE_SHARE = getIcon("rad_share", true);

    public static Icon CHECK_ICON = getIcon("check", false);

    private static boolean isNewUiEnabled() {
        return Registry.get("ide.experimental.ui").asBoolean();
    }

    private static Icon getIcon(String iconName, boolean existInNewUi) {
        if (isNewUiEnabled() && existInNewUi) {
            iconName = iconName + "_new_ui";
        }
        iconName = iconName + "." + "svg";
        return IconLoader.getIcon(ICON_PATH + iconName, RadicleIcons.class);
    }

}
