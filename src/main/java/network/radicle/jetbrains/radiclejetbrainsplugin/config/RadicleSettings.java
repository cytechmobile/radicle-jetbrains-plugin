package network.radicle.jetbrains.radiclejetbrainsplugin.config;

public class RadicleSettings {
    private String path;

    public RadicleSettings() {
        this("");
    }

    public RadicleSettings(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "RadiclePath{" +
                "path='" + path + '\'' +
                '}';
    }
}
