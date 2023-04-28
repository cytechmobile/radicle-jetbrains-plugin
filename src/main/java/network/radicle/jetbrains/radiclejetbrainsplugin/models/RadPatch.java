package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import git4idea.repo.GitRepository;

import java.time.Instant;
import java.util.List;

public class RadPatch {
    public GitRepository repo;
    public String defaultBranch;
    public String projectId;
    public String id;
    public String title;
    public Author author;
    public String description;
    public String target;
    public List<String> tags;
    public State state;
    public List<Revision> revisions;

    public RadPatch() {
        // for json
    }

    public RadPatch(
            String id, String title, Author author, String description, String target, List<String> tags, State state, List<Revision> revisions) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.target = target;
        this.tags = tags;
        this.state = state;
        this.revisions = revisions;
    }

    public record Revision(
            String id, String description, String base, String oid, List<String> refs,
            List<Merge> merges, Instant timestamp, List<Discussion> discussions, List<String> reviews) { }

    public record Merge(String node, String commit, Instant timestamp) { }

    public record Discussion(String id, Author author, String body, Instant timestamp, String replyTo, List<String> reactions) { }

    public record Author(String id) { }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum State {
        OPEN("Open"),
        CLOSED("Closed"),
        MERGED("Merged"),
        ARCHIVED("Archived");

        public final String status;

        State(String status) {
            this.status = status;
        }

        @JsonCreator
        public static State forValues(@JsonProperty("status") String status) {
            for (State state : State.values()) {
                if (state.status.equalsIgnoreCase(status)) {
                    return state;
                }
            }
            return null;
        }
    }
}
