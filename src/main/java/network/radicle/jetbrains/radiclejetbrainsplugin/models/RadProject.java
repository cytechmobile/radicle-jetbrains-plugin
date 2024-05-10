package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import java.util.List;

public class RadProject {
    public String id;
    public String name;
    public String description;
    public String defaultBranch;
    public List<RadAuthor> delegates;
    public String head;

    public RadProject() {
        // for json
    }

    public RadProject(String id, String name, String description, String defaultBranch, List<RadAuthor> delegates) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultBranch = defaultBranch;
        this.delegates = delegates;
    }
}
