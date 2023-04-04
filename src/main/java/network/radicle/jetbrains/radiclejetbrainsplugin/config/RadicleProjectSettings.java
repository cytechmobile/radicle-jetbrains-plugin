package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

public class RadicleProjectSettings {
    private String radHome;
    private String path;
    private SeedNode seedNode;

    public RadicleProjectSettings(String radHome, String path, SeedNode seedNode) {
        this.radHome = radHome;
        this.path = path;
        this.seedNode = seedNode;
    }

    public String getRadHome() {
        return radHome;
    }

    public String getPath() {
        return path;
    }

    public SeedNode getSeedNode() {
        return seedNode;
    }

    @Override
    public String toString() {
        return "RadicleProjectSettings{" +
                "radHome='" + radHome + '\'' +
                "path='" + path + '\'' +
                "seedNode='" + seedNode + '\'' +
                '}';
    }
}
