package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

public class RadAuthor {
    public String id;
    public String alias;

    @JsonCreator // needed when author is represented only by a string (id), e.g. inside rad patch edits
    protected RadAuthor(String id) {
        this(id, null);
    }

    @JsonCreator
    public RadAuthor(@JsonProperty("id") String id, @JsonProperty("alias") String alias) {
        this.id = id;
        this.alias = alias;
    }

    public String generateLabelText() {
        if (!Strings.isNullOrEmpty(alias)) {
            return alias;
        }
        return Utils.formatDid(id);
    }

    public String generateLabelText(RadicleCliService rad) {
        tryResolveAlias(rad);
        return generateLabelText();
    }

    @Override
    public String toString() {
        return "{\"id\": " + id + "\", \"alias\": \"" + alias + "\"}";
    }

    public void tryResolveAlias(RadicleCliService rad) {
        if (Strings.isNullOrEmpty(alias) && rad != null) {
            alias = Strings.nullToEmpty(rad.getAlias(id));
        }
    }

    public boolean contains(RadicleCliService rad, String query) {
        tryResolveAlias(rad);
        final var cq = Strings.nullToEmpty(query).toLowerCase();
        return Strings.nullToEmpty(id).toLowerCase().contains(cq) || Strings.nullToEmpty(alias).toLowerCase().contains(cq);
    }
}
