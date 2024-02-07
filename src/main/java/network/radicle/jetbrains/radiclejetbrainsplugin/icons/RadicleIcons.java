package network.radicle.jetbrains.radiclejetbrainsplugin.icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.registry.Registry;

import javax.swing.Icon;

public class  RadicleIcons {
    private static final String ICON_PATH = "/icons/";

    public static final Icon DEFAULT_AVATAR = getIcon("default_avatar", false, "svg");

    public static final Icon RADICLE = getIcon("radicle", false, "svg");

    public static final Icon RADICLE_TOOL_WINDOW = getIcon("rad_tool_window", true, "svg");

    public static final Icon RADICLE_FETCH = getIcon("rad_fetch", true, "svg");

    public static final Icon RADICLE_CLONE = getIcon("rad_clone", true, "svg");

    public static final Icon RADICLE_SHARE = getIcon("rad_share", true, "svg");

    public static final Icon RADICLE_PULL = getIcon("rad_pull", true, "svg");

    public static final Icon RADICLE_SYNC = getIcon("rad_sync", true, "svg");

    public static final Icon EMOJI = getIcon("emoji", false, "svg");

    private static boolean isNewUiEnabled() {
        return Registry.get("ide.experimental.ui").asBoolean();
    }

    private static Icon getIcon(String iconName, boolean existInNewUi, String extension) {
        if (isNewUiEnabled() && existInNewUi) {
            iconName = iconName + "_new_ui";
        }
        iconName = iconName + "." + extension;
        return IconLoader.getIcon(ICON_PATH + iconName, RadicleIcons.class);
    }

}
