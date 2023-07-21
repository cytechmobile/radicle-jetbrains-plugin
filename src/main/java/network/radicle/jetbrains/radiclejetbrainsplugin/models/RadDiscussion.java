package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import java.time.Instant;
import java.util.List;

public class RadDiscussion {
    public String id;
    public RadAuthor author;
    public String body;
    public Instant timestamp;
    public String replyTo;
    public List<List<String>> reactions;

    public RadDiscussion() {
    }

    public RadDiscussion(String id, RadAuthor author, String body, Instant timestamp,
                         String replyTo, List<List<String>> reactions) {
        this.id = id;
        this.author = author;
        this.body = body;
        this.timestamp = timestamp;
        this.replyTo = replyTo;
        this.reactions = reactions;
    }
}
