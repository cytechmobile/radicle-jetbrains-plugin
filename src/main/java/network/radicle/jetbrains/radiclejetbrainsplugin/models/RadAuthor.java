package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.google.common.base.Strings;

public class RadAuthor {
    public String id;
    public String alias;

    public RadAuthor() {
    }

    public RadAuthor(String id) {
        this.id = id;
    }

    public RadAuthor(String id, String alias) {
        this.id = id;
        this.alias = alias;
    }

    public String generateLabelText() {
        if (!Strings.isNullOrEmpty(alias)) {
            return alias;
        }
        return id;
    }

    @Override
    public String toString() {
        return "{\"id\": " + id + "\", \"alias\": \"" + alias + "\"}";
    }
}
