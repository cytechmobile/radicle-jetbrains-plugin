package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import git4idea.repo.GitRepository;

import java.time.Instant;
import java.util.List;

public class RadIssue {
    public String id;
    public Author author;
    public String title;
    public State state;
    public List<Object> assignees;
    public List<String> tags;
    public List<Discussion> discussion;
    public GitRepository repo;

    public RadIssue() {
        // for json
    }

    public RadIssue(String id, Author author, String title, State state,
                    List<Object> assignees, List<String> tags, List<Discussion> discussion) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.state = state;
        this.assignees = assignees;
        this.tags = tags;
        this.discussion = discussion;
    }

    public record Discussion(String id, RadIssue.Author author, String body, Instant timestamp, String replyTo, List<String> reactions) { }
    public record Author(String id) { }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum State {
        OPEN("Open"),
        CLOSED("Closed");

        public final String status;

        State(String status) {
            this.status = status;
        }

        @JsonCreator
        public static RadIssue.State forValues(@JsonProperty("status") String status) {
            for (RadIssue.State state : RadIssue.State.values()) {
                if (state.status.equalsIgnoreCase(status)) {
                    return state;
                }
            }
            return null;
        }
    }
}
