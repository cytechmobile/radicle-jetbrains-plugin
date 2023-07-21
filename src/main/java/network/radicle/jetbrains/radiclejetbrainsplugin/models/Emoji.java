package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import java.util.List;

public class Emoji {
    private final String name;
    private final String unicode;

    public Emoji(String name, String unicode) {
        this.name = name;
        this.unicode = unicode;
    }

    public String getName() {
        return name;
    }

    public String getUnicode() {
        return unicode;
    }

    public static List<Emoji> loadEmojis() {
        return List.of(
                new Emoji("Smiley Face", "\uD83D\uDE00"), // 😀
                new Emoji("Heart", "\u2764"), // ❤
                new Emoji("Thumbs Up", "\uD83D\uDC4D"), // 👍
                new Emoji("Crying Face", "\uD83D\uDE22") // 😢
        );
    }
}
