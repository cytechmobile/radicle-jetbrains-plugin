package network.radicle.jetbrains.radiclejetbrainsplugin.config;

public class RadicleProjectSettings {
    private String radHome;
    private String path;

    public RadicleProjectSettings(String radHome, String path) {
        this.radHome = radHome;
        this.path = path;
    }

    public String getRadHome() {
        return radHome;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "RadicleProjectSettings{" +
                "radHome='" + radHome + '\'' +
                "path='" + path + '\'' +
                '}';
    }
}
