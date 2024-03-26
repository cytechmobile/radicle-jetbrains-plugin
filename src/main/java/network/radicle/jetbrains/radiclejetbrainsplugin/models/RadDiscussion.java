package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadDiscussion implements RadPatch.TimelineEvent {
    public String id;
    public RadAuthor author;
    public String body;
    public Instant timestamp;
    public String replyTo;
    public List<Reaction> reactions;
    public List<Embed> embeds;
    public Location location;

    public RadDiscussion() {
    }

    public RadDiscussion(String id, RadAuthor author, String body, Instant timestamp,
                         String replyTo, List<Reaction> reactions, List<Embed> embeds, Location location) {
        this.id = id;
        this.author = author;
        this.body = body;
        this.timestamp = timestamp;
        this.replyTo = replyTo;
        this.reactions = reactions;
        this.embeds = embeds;
        this.location = location;
    }

    public boolean isReviewComment() {
        return location != null;
    }

    public Reaction findReaction(String emojiUnicode) {
        return reactions.stream().filter(r -> r.emoji().equals(emojiUnicode)).findFirst().orElse(null);
    }

    @Override
    public Instant getTimestamp() {
        return this.timestamp;
    }

    public static class Location {
        public String path;
        public String commit;
        public String type;
        public int start;
        public int end;

        public Location() {
        }

        public Location(String path, String type, String commit, int start, int end) {
            this.path = path;
            this.type = type;
            this.start = start;
            this.end = end;
            this.commit = commit;
        }

        public Map<String, Object> getMapObject() {
            return Map.of("path",  path, "commit", commit, "new",
                    Map.of("type", "lines", "range", Map.of("start", start, "end", end)));
        }

        @JsonProperty("new")
        private void unpackNewObject(Map<String, Object> line) {
            var range = (HashMap<String, Integer>) line.get("range");
            type = (String) line.get("type");
            start = range.get("start");
            end = range.get("end");
        }
    }
}
