package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class RadIssue {
    private static final Logger logger = Logger.getInstance(RadIssue.class);

    public String id;
    public RadAuthor author;
    public String title;
    public State state;
    public List<RadAuthor> assignees;
    public List<String> labels;
    @JsonDeserialize(using = DiscussionDeserializer.class)
    @JsonProperty("thread")
    public List<RadDiscussion> discussion;
    public GitRepository repo;
    public Project project;
    public String projectId;
    public SeedNode seedNode;

    public RadIssue() {
        // for json
    }

    public RadIssue(String id, RadAuthor author, String title, State state, List<RadAuthor> assignees, List<String> labels, List<RadDiscussion> discussion) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.state = state;
        this.assignees = assignees;
        this.labels = labels;
        this.discussion = discussion;
    }

    public RadIssue(RadIssue other) {
        this.id = other.id;
        this.author = other.author;
        this.title = other.title;
        this.state = other.state;
        this.assignees = other.assignees;
        this.labels = other.labels;
        this.discussion = other.discussion;
        this.repo = other.repo;
        this.project = other.project;
        this.projectId = other.projectId;
        this.seedNode = other.seedNode;
    }

    public RadDiscussion findDiscussion(String commentId) {
        return discussion.stream().filter(disc -> disc.id.equals(commentId)).findFirst().orElse(null);
    }

    @JsonIgnore
    public String getDescription() {
        return discussion == null || discussion.isEmpty() ? "" : discussion.get(0).body;
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum State {
        OPEN("open", "Open", "--open"),
        CLOSED("closed", "Closed", "--closed");

        public final String status;
        public final String label;
        public final String cli;

        State(String status, String label, String cli) {
            this.status = status;
            this.label = label;
            this.cli = cli;
        }

        @JsonCreator
        public static RadIssue.State forValues(@JsonProperty("status") String status) {
            for (RadIssue.State state : RadIssue.State.values()) {
                if (state.status.equals(status)) {
                    return state;
                }
            }
            return null;
        }
    }

    public static class DiscussionDeserializer extends StdDeserializer<List<RadDiscussion>> {
        protected DiscussionDeserializer() {
            super(RadDiscussion.class);
        }

        @Override
        public List<RadDiscussion> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            var discussions = new ArrayList<RadDiscussion>();
            try {
                var json = jsonParser.getCodec().readTree(jsonParser);
                var comments = json.get("comments");
                for (Iterator<String> it = comments.fieldNames(); it.hasNext();) {
                    var key = it.next();
                    var comment = comments.get(key);
                    var discussion = RadicleCliService.MAPPER.readValue(comment.toString(), new TypeReference<RadDiscussion>() { });
                    discussion.id = key;
                    discussions.add(discussion);
                }
            } catch (Exception e) {
                logger.warn("Unable to deserialize rad issue");
            }
            discussions.sort(Comparator.comparing(RadDiscussion::getTimestamp));
            return discussions;
        }
    }
}
