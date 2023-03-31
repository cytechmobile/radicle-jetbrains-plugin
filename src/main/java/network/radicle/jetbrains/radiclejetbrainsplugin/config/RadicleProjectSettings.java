package network.radicle.jetbrains.radiclejetbrainsplugin.config;

public class RadicleProjectSettings {
    private String radHome;

    public RadicleProjectSettings(String radHome) {
        this.radHome = radHome;
    }

    public String getRadHome() {
        return radHome;
    }

    @Override
    public String toString() {
        return "RadicleProjectSettings{" +
                "path='" + radHome + '\'' +
                '}';
    }
}
