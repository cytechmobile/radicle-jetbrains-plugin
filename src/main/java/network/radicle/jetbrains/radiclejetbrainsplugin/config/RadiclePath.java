package network.radicle.jetbrains.radiclejetbrainsplugin.config;

public class RadiclePath {
    private String path;

    public RadiclePath() {
        this("");
    }

    public RadiclePath(String path) {
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
