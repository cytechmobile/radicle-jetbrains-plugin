package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

public class RadicleSettings {
    private String path;
    private String radHome;
    private SeedNode seedNode;

    public RadicleSettings(String path, SeedNode seedNodes, String radHome) {
        this.path = path;
        this.seedNode = seedNodes;
        this.radHome = radHome;
    }

    public String getPath() {
        return path;
    }

    public String getRadHome() {
        return radHome;
    }

    public SeedNode getSeedNode() {
        return seedNode;
    }

    @Override
    public String toString() {
        return "RadicleSettings{" +
                "path='" + path + '\'' +
                '}';
    }
}
