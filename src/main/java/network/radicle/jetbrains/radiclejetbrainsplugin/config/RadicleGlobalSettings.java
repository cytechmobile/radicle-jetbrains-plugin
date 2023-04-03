package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

public class RadicleGlobalSettings {
    private String path;
    private SeedNode seedNode;

    public RadicleGlobalSettings(String path, SeedNode seedNode) {
        this.path = path;
        this.seedNode = seedNode;
    }

    public String getPath() {
        return path;
    }

    public SeedNode getSeedNode() {
        return seedNode;
    }

    @Override
    public String toString() {
        return "RadicleGlobalSettings{" +
                "path='" + path + '\'' +
                '}';
    }
}
