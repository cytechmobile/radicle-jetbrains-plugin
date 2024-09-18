package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import java.util.List;

public record Reaction(String emoji, List<RadAuthor> authors) {
    public RadAuthor findAuthor(String did) {
        return authors.stream().filter(r -> did.contains(r.id)).findFirst().orElse(null);
    }
}
