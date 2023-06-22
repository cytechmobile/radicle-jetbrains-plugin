package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;

import java.util.List;

public class RadIssue {
    public String id;
    public RadAuthor author;
    public String title;
    public State state;
    public List<Object> assignees;
    public List<String> tags;
    public List<RadDiscussion> discussion;
    public GitRepository repo;
    public Project project;
    public String projectId;
    public SeedNode seedNode;

    public RadIssue() {
        // for json
    }

    public RadIssue(String id, RadAuthor author, String title, State state,
                    List<Object> assignees, List<String> tags, List<RadDiscussion> discussion) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.state = state;
        this.assignees = assignees;
        this.tags = tags;
        this.discussion = discussion;
    }

    public RadIssue(RadIssue other) {
        this.id = other.id;
        this.author = other.author;
        this.title = other.title;
        this.state = other.state;
        this.assignees = other.assignees;
        this.tags = other.tags;
        this.discussion = other.discussion;
        this.repo = other.repo;
        this.project = other.project;
        this.projectId = other.projectId;
        this.seedNode = other.seedNode;
    }

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
