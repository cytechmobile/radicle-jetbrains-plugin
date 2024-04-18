package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Strings;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RadPatch {
    private static final Logger logger = LoggerFactory.getLogger(RadPatch.class);

    public SeedNode seedNode;
    public GitRepository repo;
    public Project project;
    public RadProject radProject;
    public RadAuthor self;

    public String id;
    public String title;
    public RadAuthor author;
    public String target;
    public List<String> labels;
    public State state;
    public List<Revision> revisions;
    public List<Merge> merges;

    public RadPatch() {
        // for json
    }

    public RadPatch(
            String id, RadProject radProject, RadAuthor self, String title, RadAuthor author, String target,
            List<String> labels, State state, List<Revision> revisions) {
        this.id = id;
        this.radProject = radProject;
        this.self = self;
        this.title = title;
        this.author = author;
        this.target = target;
        this.labels = labels;
        this.state = state;
        this.revisions = revisions;
    }

    public RadPatch(RadPatch other) {
        this.seedNode = other.seedNode;
        this.repo = other.repo;
        this.project = other.project;
        this.radProject = other.radProject;
        this.self = other.self;
        this.id = other.id;
        this.title = other.title;
        this.author = other.author;
        this.target = other.target;
        this.labels = other.labels;
        this.state = other.state;
        this.revisions = other.revisions;
        this.merges = other.merges;
    }

    public Map<String, List<GitCommit>> calculateCommits() {
        var myRevisions = new HashMap<String, List<GitCommit>>();
        var success = fetchCommits();
        if (!success) {
            return null;
        }
        try {
            for (var rev : this.revisions) {
                try {
                    var patchCommits = GitHistoryUtils.history(this.repo.getProject(),
                            this.repo.getRoot(), rev.base() + "..." + rev.oid());
                    myRevisions.put(rev.id(), patchCommits);
                } catch (Exception e) {
                    logger.warn("error calculating patch commits for revision: {} in patch: {}", rev.id(), this, e);
                    myRevisions.put(rev.id(), new ArrayList<>());
                }
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

    @JsonIgnore
    public boolean isMerged() {
        return this.state == State.MERGED;
    }

    public String findRevisionId(String commentId) {
        for (var revision : revisions) {
            var found = revision.discussions.stream().anyMatch(d -> d.id.equals(commentId));
            if (found) {
                return revision.id;
            }
        }
        return "";
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

    @JsonIgnore
    public Revision getLatestRevision() {
        if (this.revisions == null || this.revisions.isEmpty()) {
            return null;
        }
        return this.revisions.get(this.revisions.size() - 1);
    }

    @JsonIgnore
    public String getLatestNonEmptyRevisionDescription() {
        for (int i = this.revisions.size() - 1; i >= 0; i--) {
            var rev = this.revisions.get(i);
            if (!Strings.isNullOrEmpty(rev.description)) {
                return Strings.nullToEmpty(rev.description);
            }
        }
        return "";
    }

    @JsonIgnore
    public List<TimelineEvent> getTimelineEvents() {
        var list = new ArrayList<TimelineEvent>();
        for (var revision : revisions) {
            list.add(revision);
            list.addAll(revision.getReviews());
            list.addAll(revision.discussions);
        }
        list.sort(Comparator.comparing(TimelineEvent::getTimestamp));
        return list;
    }

    public Revision findRevision(String revisionId) {
       return revisions.stream().filter(rev -> rev.id().equals(revisionId)).findFirst().orElse(null);
    }

    public record Revision(
            String id, RadAuthor author, String description, List<Edit> edits, List<Reaction> reactions, String base, String oid, List<String> refs,
            Instant timestamp, List<RadDiscussion> discussions, List<Review> reviews) implements TimelineEvent {
        @Override
        public Instant getTimestamp() {
            return timestamp;
        }

        public List<Review> getReviews() {
            Set<String> seen = new HashSet<>();
            var myReviews = new ArrayList<>(reviews);
            // Remove the duplicates reviews. We can only have 1 review per author per revision
            myReviews.removeIf(e -> !seen.add(e.author.id));
            return myReviews;
        }

        public RadDiscussion findDiscussion(String commentId) {
            return discussions().stream().filter(disc -> disc.id.equals(commentId)).findFirst().orElse(null);
        }
    }

    public record Edit(RadAuthor author, String body, Instant timestamp, List<Embed> embeds) {}

    public record Merge(RadAuthor author, String commit, Instant timestamp, String revision) { }

    public record Review(String id, RadAuthor author, Verdict verdict, String summary,
                         List<RadDiscussion> comments, Instant timestamp) implements TimelineEvent {
        @Override
        public Instant getTimestamp() {
            return timestamp;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public enum Verdict {
            ACCEPT("accept"),
            REJECT("reject");

            private final String value;

            Verdict(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }

            @JsonCreator
            public static Verdict forValues(String val) {
                for (Verdict verdict : Verdict.values()) {
                    if (verdict.value.equals(val.toLowerCase())) {
                        return verdict;
                    }
                }
                return null;
            }
        }
    }


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

    public interface TimelineEvent {
        Instant getTimestamp();
    }
}
