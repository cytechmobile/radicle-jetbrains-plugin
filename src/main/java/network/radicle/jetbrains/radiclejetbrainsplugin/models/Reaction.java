package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Reaction {
    public String nid;
    public String emoji;

    public Reaction() {
    }

    public Reaction(String nid, String emoji) {
        this.nid = nid;
        this.emoji = emoji;
    }

    public static class Deserializer extends StdDeserializer<List<Reaction>> {
        private static final Logger logger = LoggerFactory.getLogger(Deserializer.class);

        protected Deserializer() {
            super(List.class);
        }

        @Override
        public List<Reaction> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            try {
                var reactions = new ArrayList<Reaction>();
                JsonNode node = jsonParser.getCodec().readTree(jsonParser);
                for (var reactionNode : node) {
                    var nid = reactionNode.get(0).asText();
                    var emoji = reactionNode.get(1).asText();
                    reactions.add(new Reaction(nid, emoji));
                }
                return reactions;
            } catch (Exception e) {
                logger.warn("Unable to deserialize reactions", e);
                return List.of();
            }
        }
    }

}
