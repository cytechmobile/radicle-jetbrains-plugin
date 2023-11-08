package network.radicle.jetbrains.radiclejetbrainsplugin.models;

public class Embed {
    private String oid;
    private String name;
    private String content;

    public Embed() {
        //For jackson
    }

    public Embed(String oid, String name, String content) {
        this.oid = oid;
        this.name = name;
        this.content = content;
    }

    public String getOid() {
        return oid;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
