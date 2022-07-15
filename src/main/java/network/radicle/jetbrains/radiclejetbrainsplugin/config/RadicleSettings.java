package network.radicle.jetbrains.radiclejetbrainsplugin.config;

public class RadicleSettings {
    private String path;
    private Boolean radSync;

    public RadicleSettings() {
        this("",null);
    }

    public RadicleSettings(String path,Boolean radSync) {
        this.path = path;
        this.radSync = radSync;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getRadSync() {
        return radSync;
    }

    public void setRadSync(Boolean radSync) {
        this.radSync = radSync;
    }

    @Override
    public String toString() {
        return "RadicleSettings{" +
                "path='" + path + '\'' +
                ", radSync=" + radSync +
                '}';
    }
}
