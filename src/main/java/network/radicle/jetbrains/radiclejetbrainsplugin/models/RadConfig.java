package network.radicle.jetbrains.radiclejetbrainsplugin.models;

public class RadConfig {
    public String gitStoragePath;
    public String keysStoragePath;

    public RadConfig(String gitStoragePath, String keysStoragePath) {
        this.gitStoragePath = gitStoragePath;
        this.keysStoragePath = keysStoragePath;
    }
}
