package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import git4idea.repo.GitRepository;

import java.time.Instant;
import java.util.List;

public class RadPatch {
    public GitRepository repo;
    public String id;
    public String title;
    public Author author;
    public String description;
    public String target;
    public List<String> tags;
    public State state;
    public List<Revision> revisions;

    public record Author(String id) {
    }

    public RadPatch(String id, String title, Author author, String description, String target, List<String> tags,
                    State state, List<Revision> revisions) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.target = target;
        this.tags = tags;
        this.state = state;
        this.revisions = revisions;
    }

    public RadPatch() {
    }

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
                if (state.status.toLowerCase().equals(status)) {
                    return state;
                }
            }
            return null;
        }
    }

    public record Revision(String id, String description, String base, String oid, List<String> refs,
                           List<Merge> merges, Instant timestamp,
                           List<Discussion> discussions, List<String> reviews) {

        @Override
        public String toString() {
            return "{" +
                    "\"id\":\"" + id + '\"' +
                    ", \"description\":\"" + description + '\"' +
                    ", \"base\":\"" + base + '\"' +
                    ", \"oid\":\"" + oid + '\"' +
                    ", \"refs\":" + refs +
                    ", \"merges\":" + merges +
                    ", \"timestamp\":" + timestamp.toEpochMilli() +
                    ", \"discussions\":" + discussions +
                    ", \"reviews\":" + reviews +
                    '}';
        }
    }

    public record Merge(String node, String commit, Instant timestamp) {
        @Override
        public String toString() {
            return "{" +
                    "\"node\":\"" + node + '\"' +
                    ", \"commit\":\"" + commit + '\"' +
                    ", \"timestamp\":" + timestamp.toEpochMilli() +
                    '}';
        }
    }

    public record Discussion(String id, Author author, String body, Instant timestamp, String replyTo,
                             List<String> reactions) {
        @Override
        public String toString() {
            return "{" +
                    "\"id\":\"" + id + '\"' +
                    ", \"author\":\"" + author +
                    ", \"body\":\"" + body + '\"' +
                    ", \"timestamp\":" + timestamp.toEpochMilli() +
                    ", \"replyTo\":\"" + replyTo + '\"' +
                    ", \"reactions\":[" + wrapWithQuotesAndJoin(reactions) +
                    '}';
        }
    }

    @Override
    public String toString() {
        return  "{" +
                "\"id\":\"" + id + '\"' +
                ", \"title\":\"" + title + '\"' +
                ", \"author\":" + "{\"id\":\"" + author.id + "\"}" +
                ", \"description\":\"" + description + '\"' +
                ", \"target\":\"" + target + '\"' +
                ", \"tags\":[" + wrapWithQuotesAndJoin(tags) +
                "], \"state\":{\"status\":\"" + state.status.toLowerCase() + "\"}" +
                ", \"revisions\":" + revisions +
                '}';
    }

    private static String wrapWithQuotesAndJoin(List<String> strings) {
        return strings.isEmpty() ? "" : "\"" + String.join("\", \"", strings) + "\"";
    }
}
