package network.radicle.jetbrains.radiclejetbrainsplugin.models;

public class RadProject {
    public String id;
    public String name;
    public String description;
    public String defaultBranch;

    public RadProject() {
        // for json
    }

    public RadProject(String id, String name, String description, String defaultBranch) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultBranch = defaultBranch;
    }
}
