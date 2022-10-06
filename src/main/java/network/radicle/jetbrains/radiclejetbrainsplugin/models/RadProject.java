package network.radicle.jetbrains.radiclejetbrainsplugin.models;

public class RadProject {
    public String urn;
    public String name;
    public String description;
    public String radUrl;

    public RadProject(String urn, String name, String description, String radUrl) {
        this.urn = urn;
        this.name = name;
        this.description = description;
        this.radUrl = radUrl;
    }
}
