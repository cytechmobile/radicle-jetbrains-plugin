package network.radicle.jetbrains.radiclejetbrainsplugin.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface RadicleIcons {
    Icon Radicle = IconLoader.getIcon("/icons/radicle.svg", RadicleIcons.class);
    Icon RadiclePush = IconLoader.getIcon("/icons/rad_push.svg", RadicleIcons.class);
    Icon RadiclePull = IconLoader.getIcon("/icons/rad_pull.svg", RadicleIcons.class);
    Icon RadicleSync = IconLoader.getIcon("/icons/rad_sync.svg", RadicleIcons.class);
    Icon CheckIcon = IconLoader.getIcon("/icons/check.svg", RadicleIcons.class);
}
