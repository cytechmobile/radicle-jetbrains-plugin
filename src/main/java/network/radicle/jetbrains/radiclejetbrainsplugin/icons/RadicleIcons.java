package network.radicle.jetbrains.radiclejetbrainsplugin.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public interface RadicleIcons {
    Icon RADICLE = IconLoader.getIcon("/icons/radicle.svg", RadicleIcons.class);
    Icon RADICLE_PUSH = IconLoader.getIcon("/icons/rad_push.svg", RadicleIcons.class);
    Icon RADICLE_PULL = IconLoader.getIcon("/icons/rad_pull.svg", RadicleIcons.class);
    Icon RADICLE_SYNC = IconLoader.getIcon("/icons/rad_sync.svg", RadicleIcons.class);
    Icon CHECK_ICON = IconLoader.getIcon("/icons/check.svg", RadicleIcons.class);
}
