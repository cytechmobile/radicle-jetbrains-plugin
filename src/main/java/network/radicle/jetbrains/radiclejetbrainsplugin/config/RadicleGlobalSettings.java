package network.radicle.jetbrains.radiclejetbrainsplugin.config;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import java.util.List;

public class RadicleGlobalSettings {
    private String path;
    private List<SeedNode> seedNodes;

    public RadicleGlobalSettings(String path, List<SeedNode> seedNodes) {
        this.path = path;
        this.seedNodes = seedNodes;
    }

    public String getPath() {
        return path;
    }

    public List<SeedNode> getSeedNodes() {
        return seedNodes;
    }

    @Override
    public String toString() {
        return "RadicleGlobalSettings{" +
                "path='" + path + '\'' +
                '}';
    }
}
