package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.List;

public class RadDiscussion {
    public String id;
    public RadAuthor author;
    public String body;
    public Instant timestamp;
    public String replyTo;
    @JsonDeserialize(using = Reaction.Deserializer.class)
    public List<Reaction> reactions;
    public List<Embed> embeds;

    public RadDiscussion() {
    }

    public RadDiscussion(String id, RadAuthor author, String body, Instant timestamp,
                         String replyTo, List<Reaction> reactions, List<Embed> embeds) {
        this.id = id;
        this.author = author;
        this.body = body;
        this.timestamp = timestamp;
        this.replyTo = replyTo;
        this.reactions = reactions;
        this.embeds = embeds;
    }
}
