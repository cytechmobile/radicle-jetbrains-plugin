package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

import java.util.List;

public class RadicleSettings {
    private String path;
    private String radHome;
    private List<SeedNode> seedNodes;

    public RadicleSettings(String path, List<SeedNode> seedNodes, String radHome) {
        this.path = path;
        this.seedNodes = seedNodes;
        this.radHome = radHome;
    }

    public String getPath() {
        return path;
    }

    public String getRadHome() {
        return radHome;
    }

    public List<SeedNode> getSeedNodes() {
        return seedNodes;
    }

    @Override
    public String toString() {
        return "RadicleSettings{" +
                "path='" + path + '\'' +
                '}';
    }
}
