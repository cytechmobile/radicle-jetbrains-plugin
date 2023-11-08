package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.project.Project;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadPatch {
    private static final Logger logger = LoggerFactory.getLogger(RadPatch.class);
    public SeedNode seedNode;
    public GitRepository repo;
    public Project project;
    public String defaultBranch;
    public String projectId;
    public String id;
    public String title;
    public RadAuthor author;
    public String description;
    public String target;
    public List<String> labels;
    public State state;
    public List<Revision> revisions;

    public RadPatch() {
        // for json
    }

    public RadPatch(
            String id, String title, RadAuthor author, String description, String target, List<String> labels, State state, List<Revision> revisions) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.target = target;
        this.labels = labels;
        this.state = state;
        this.revisions = revisions;
    }

    public RadPatch(RadPatch other) {
        this.seedNode = other.seedNode;
        this.repo = other.repo;
        this.project = other.project;
        this.defaultBranch = other.defaultBranch;
        this.projectId = other.projectId;
        this.id = other.id;
        this.title = other.title;
        this.author = other.author;
        this.description = other.description;
        this.target = other.target;
        this.labels = other.labels;
        this.state = other.state;
        this.revisions = other.revisions;
    }

    public record Revision(
            String id, String description, String base, String oid, List<String> refs,
            List<Merge> merges, Instant timestamp, List<RadDiscussion> discussions, List<Object> reviews) { }

    public record Merge(String node, String commit, Instant timestamp) { }


    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum State {
        OPEN("open", "Open"),
        DRAFT("draft", "Draft"),
        ARCHIVED("archived", "Archived"),
        MERGED("merged", "Merged");

        public final String status;
        public final String label;

        State(String status, String label) {
            this.status = status;
            this.label = label;
        }

        @JsonCreator
        public static State forValues(@JsonProperty("status") String status) {
            for (State state : State.values()) {
                if (state.status.equals(status)) {
                    return state;
                }
            }
            return null;
        }
    }

    public Map<String, List<GitCommit>> calculateCommits() {
        var myRevisions = new HashMap<String, List<GitCommit>>();
        var success = fetchCommits();
        if (!success) {
            return null;
        }
        try {
            for (var rev : this.revisions) {
                var patchCommits = GitHistoryUtils.history(this.repo.getProject(),
                        this.repo.getRoot(), rev.base() + "..." + rev.oid());
                myRevisions.put(rev.id(), patchCommits);
            }
            return myRevisions;
        } catch (Exception e) {
            logger.warn("error calculating patch commits for patch: {}", this, e);
            return null;
        }
    }

    private boolean fetchCommits() {
        var service = this.repo.getProject().getService(RadicleProjectService.class);
        var output = service.fetchPeerChanges(this.repo);
        return RadAction.isSuccess(output);
    }

    public boolean isMerged() {
        return this.state.status.equals(State.MERGED.status);
    }

    @Override
    public String toString() {
        try {
            var p = new RadPatch(this);
            p.repo = null;
            p.project = null;
            return RadicleProjectApi.MAPPER.writeValueAsString(p);
        } catch (Exception e) {
            logger.warn("error converting this to string", e);
            return "";
        }
    }
}
