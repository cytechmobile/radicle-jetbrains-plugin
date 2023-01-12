package network.radicle.jetbrains.radiclejetbrainsplugin.icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.registry.Registry;

import javax.swing.*;

public interface RadicleIcons {
    Icon Radicle =  IconLoader.getIcon("/icons/radicle.svg", RadicleIcons.class);
    Icon RadicleToolWindow = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_tool_window_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/radicle.svg", RadicleIcons.class);
    Icon RadiclePush = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_push_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_push.svg", RadicleIcons.class);
    Icon RadiclePull = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_pull_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_pull.svg", RadicleIcons.class);
    Icon RadicleSync = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_sync_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_sync.svg", RadicleIcons.class);
    Icon RadicleClone = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_clone_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_clone.svg", RadicleIcons.class);
    Icon RadicleShare = Registry.get("ide.experimental.ui").asBoolean() ? IconLoader.getIcon("/icons/rad_share_new_ui.svg", RadicleIcons.class) :
            IconLoader.getIcon("/icons/rad_share.svg", RadicleIcons.class);
    Icon CheckIcon = IconLoader.getIcon("/icons/check.svg", RadicleIcons.class);
}
