package network.radicle.jetbrains.radiclejetbrainsplugin.config;

public class RadicleSettings {
    private String path;
    private String radSync;

    public RadicleSettings() {
        this("",null);
    }

    public RadicleSettings(String path,String radSync) {
        this.path = path;
        this.radSync = radSync;
    }

    public String getPath() {
        return path;
    }

    public String getRadSync() {
        return radSync;
    }

    @Override
    public String toString() {
        return "RadicleSettings{" +
                "path='" + path + '\'' +
                ", radSync=" + radSync +
                '}';
    }
}
