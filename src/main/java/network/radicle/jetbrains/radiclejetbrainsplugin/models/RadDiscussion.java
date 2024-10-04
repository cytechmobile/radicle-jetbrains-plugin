package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.base.Strings;
import com.intellij.openapi.diagnostic.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadDiscussion implements RadPatch.TimelineEvent {
    private static final Logger logger = Logger.getInstance(RadDiscussion.class);

    public String id;
    public RadAuthor author;
    public String body;
    public Instant timestamp;
    public String replyTo;
    @JsonDeserialize(using = RadDiscussion.ReactionDeserializer.class)
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

    public boolean isReply() {
        return !Strings.isNullOrEmpty(replyTo);
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

        @JsonIgnore
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

    public static class ReactionDeserializer extends StdDeserializer<List<Reaction>> {
        protected ReactionDeserializer() {
            super(Reaction.class);
        }

        @Override
        public List<Reaction> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            var reactions = new ArrayList<Reaction>();
            try {
                JsonNode json = jsonParser.getCodec().readTree(jsonParser);
                for (var node : json) {
                    var fromApi = !node.isArray();
                    if (fromApi) {
                        //Deserialize the reactions that we get from api
                        var emoji = node.get("emoji").asText();
                        var authors = node.get("authors");
                        var myAuthors = new ArrayList<RadAuthor>();
                        for (var author : authors) {
                            var id = author.get("id").asText();
                            var alias = author.get("alias").asText();
                            myAuthors.add(new RadAuthor(id, alias));
                        }
                        reactions.add(new Reaction(emoji, myAuthors));
                    } else {
                        //Deserialize the reactions that we get from cli
                        var id = node.get(0).asText();
                        var emoji = node.get(1).asText();
                        var reaction = reactions.stream()
                                .filter(r -> r.emoji().equals(emoji))
                                .findFirst()
                                .orElse(null);
                        if (reaction == null) {
                            List<RadAuthor> authors = new ArrayList<>();
                            authors.add(new RadAuthor(id));
                            reactions.add(new Reaction(emoji, authors));
                        } else {
                            reaction.authors().add(new RadAuthor(id));
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Unable to deserialize reactions", e);
            }
            return reactions;
        }
    }
}
