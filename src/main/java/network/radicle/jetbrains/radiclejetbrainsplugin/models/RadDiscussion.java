package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadDiscussion {
    public String id;
    public RadAuthor author;
    public String body;
    @JsonDeserialize(using = TimestampDeserializer.class)
    public Instant timestamp;
    public String replyTo;
    @JsonDeserialize(using = Reaction.Deserializer.class)
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

    public static class TimestampDeserializer extends StdDeserializer<Instant> {
        private static final Logger logger = LoggerFactory.getLogger(TimestampDeserializer.class);

        protected TimestampDeserializer() {
            super(Instant.class);
        }

        @Override
        public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            try {
                JsonNode millis = jsonParser.getCodec().readTree(jsonParser);
                return Instant.ofEpochMilli(Long.parseLong(millis.toString()));
            } catch (Exception e) {
                logger.warn("Unable to deserialize timestamp", e);
                return null;
            }
        }
    }

    public static class Location {
        public String path;
        public Object old;
        public String type;
        public int start;
        public int end;

        public Location() {
        }

        public Location(String path, String type, int start, int end) {
            this.path = path;
            this.type = type;
            this.start = start;
            this.end = end;
        }

        public Map<String, Object> getMapObject() {
            return Map.of("path",  path, "new",
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
