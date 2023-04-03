package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleGlobalSettingsHandler;

import java.util.ArrayList;
import java.util.List;

public class SeedNode {
    public String url;

    public SeedNode(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
