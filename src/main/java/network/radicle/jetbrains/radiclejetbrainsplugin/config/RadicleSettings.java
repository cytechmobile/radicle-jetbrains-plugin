package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

import java.util.Arrays;
import java.util.List;

public class RadicleSettings {
    private String path;
    private Integer radSync;
    private List<SeedNode> seedNodes;

    public enum RadSyncType {
        ASK(0,"Ask"), YES(1,"Yes"), NO(2,"No");
        public final int val;
        public final String name;

        RadSyncType(int val, String name) {
            this.val = val;
            this.name = name;
        }

        public static RadSyncType from(int val) {
            return Arrays.stream(RadSyncType.values()).filter(t -> t.val == val).findFirst().orElse(null);
        }
    }

    public RadicleSettings(String path,Integer radSync, List<SeedNode> seedNodes) {
        this.path = path;
        this.radSync = radSync;
        this.seedNodes = seedNodes;
    }

    public String getPath() {
        return path;
    }

    public Integer getRadSync() {
        return radSync;
    }

    public List<SeedNode> getSeedNodes() {
        return seedNodes;
    }

    @Override
    public String toString() {
        return "RadicleSettings{" +
                "path='" + path + '\'' +
                ", radSync=" + radSync +
                '}';
    }
}
