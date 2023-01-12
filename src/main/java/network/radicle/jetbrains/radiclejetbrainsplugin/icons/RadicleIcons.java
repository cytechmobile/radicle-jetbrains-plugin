package network.radicle.jetbrains.radiclejetbrainsplugin.icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.registry.Registry;

import javax.swing.Icon;

public interface RadicleIcons {
    Icon RADICLE =  IconLoader.getIcon("/icons/radicle.svg", RadicleIcons.class);
    Icon RADICLE_TOOL_WINDOW = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_tool_window_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/radicle.svg", RadicleIcons.class);
    Icon RADICLE_PUSH = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_push_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_push.svg", RadicleIcons.class);
    Icon RADICLE_PULL = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_pull_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_pull.svg", RadicleIcons.class);
    Icon RADICLE_SYNC = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_sync_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_sync.svg", RadicleIcons.class);
    Icon RADICLE_CLONE = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_clone_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_clone.svg", RadicleIcons.class);
    Icon RADICLE_SHARE = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_share_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_share.svg", RadicleIcons.class);
    Icon CHECK_ICON = IconLoader.getIcon("/icons/check.svg", RadicleIcons.class);
}
