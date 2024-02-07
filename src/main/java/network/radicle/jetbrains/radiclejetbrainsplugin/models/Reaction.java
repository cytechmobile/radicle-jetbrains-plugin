package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import java.util.List;

public record Reaction(String emoji, List<String> authors) {
}
