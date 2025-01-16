package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RadPatch {
    private static final Logger logger = LoggerFactory.getLogger(RadPatch.class);

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
    public Map<String, Revision> revisions;
    @JsonIgnore
    public List<Merge> merges;

    public RadPatch() {
        // for json
    }

    public RadPatch(
            String id, RadProject radProject, RadAuthor self, String title, RadAuthor author, String target,
            List<String> labels, State state, Map<String, Revision> revisions) {
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

    public boolean fetchCommits() {
        var service = this.repo.getProject().getService(RadicleProjectService.class);
        var output = service.fetchPeerChanges(this.repo);
        return RadAction.isSuccess(output);
    }

    @JsonIgnore
    public boolean isMerged() {
        return this.state == State.MERGED;
    }

    public String findRevisionId(String commentId) {
        for (var revision : getRevisionList()) {
            var found = revision.getDiscussions().stream().anyMatch(d -> d.id.equals(commentId));
            if (found) {
                return revision.id;
            }
        }
        return "";
    }

    public boolean isDiscussionBelongedToLatestRevision(RadDiscussion disc) {
        var revisionId = findRevisionId(disc.id);
        return revisionId.equals(getLatestRevision().id);
    }

    @Override
    public String toString() {
        try {
            var p = new RadPatch(this);
            p.repo = null;
            p.project = null;
            return RadicleCliService.MAPPER.writeValueAsString(p);
        } catch (Exception e) {
            logger.warn("error converting this to string", e);
            return "";
        }
    }

    @JsonIgnore
    public Revision getLatestRevision() {
        var myRevisions = getRevisionList();
        if (myRevisions == null || myRevisions.isEmpty()) {
            return null;
        }
        return myRevisions.getLast();
    }

    @JsonIgnore
    public String getLatestNonEmptyRevisionDescription() {
        var myRevisions = getRevisionList();
        for (int i = myRevisions.size() - 1; i >= 0; i--) {
            var rev = myRevisions.get(i);
            if (!Strings.isNullOrEmpty(rev.getDescription())) {
                return Strings.nullToEmpty(rev.getDescription());
            }
        }
        return "";
    }

    @JsonIgnore
    public List<Revision> getRevisionList() {
        if (revisions == null || revisions.isEmpty()) {
            return new ArrayList<>();
        }
        return this.revisions.keySet().stream()
                .map(revId -> this.revisions.get(revId))
                .filter(Objects::nonNull)
                .filter(r -> r.timestamp != null)
                .sorted(Comparator.comparing(Revision::getTimestamp))
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<TimelineEvent> getTimelineEvents() {
        var list = new ArrayList<TimelineEvent>();
        for (var revision : getRevisionList()) {
            if (revision.timestamp == null) {
                continue;
            }
            list.add(revision);
            list.addAll(revision.getReviewList());
            list.addAll(revision.getDiscussions());
        }
        list.sort(Comparator.comparing(TimelineEvent::getTimestamp));
        return list;
    }

    public Revision findRevision(String revisionId) {
       return getRevisionList().stream().filter(rev -> rev.id().equals(revisionId)).findFirst().orElse(null);
    }

    public static class DiscussionObj {
        public Map<String, RadDiscussion> comments;
        public List<String> timeline;

        public DiscussionObj() {
        }

        public DiscussionObj(Map<String, RadDiscussion> comments,
                             List<String> timeline) {
            this.comments = comments;
            this.timeline = timeline;
        }

        public void setComments(Map<String, RadDiscussion> myComments) {
            for (var discId : myComments.keySet()) {
                if (Strings.isNullOrEmpty(discId)) {
                    continue;
                }
                var discussion = myComments.get(discId);
                if (discussion != null) {
                    discussion.id = discId;
                }
            }
            this.comments = myComments;
        }
    }

    public static class ReviewsDeserializer extends JsonDeserializer<Map<String, Review>> {
        @Override
        public Map<String, Review> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            //manually deserialize map of reviews, as 1.0.0 contained a list of reviews, while 1.1.0 contains a single review per reviewer
            var tree = jsonParser.getCodec().readTree(jsonParser);
            Map<String, Review> reviews = new HashMap<>();
            tree.fieldNames().forEachRemaining(fn -> {
                var rev = tree.get(fn);
                if (rev.isArray()) {
                    rev = rev.get(0);
                }
                try {
                    Review review = RadicleCliService.MAPPER.treeToValue(rev, Review.class);
                    reviews.put(fn, review);
                } catch (Exception e) {
                    logger.warn("error reading Review from tree: {}", rev, e);
                }
            });

            return reviews;
        }
    }

    public record Revision(
            String id, RadAuthor author,
            @JsonProperty("description")
            List<Edit> edits,
            List<Reaction> reactions, String base, String oid, List<String> refs,
            Instant timestamp,
            DiscussionObj discussion,
            @JsonDeserialize(using = ReviewsDeserializer.class)
            Map<String, Review> reviews) implements TimelineEvent {
        @Override
        public Instant getTimestamp() {
            return timestamp == null ? Instant.now() : timestamp;
        }

        public List<Review> getReviewList() {
            Set<String> seen = new HashSet<>();
            var myReviews = new ArrayList<Review>();
            for (var reviewId : reviews.keySet()) {
                var review = reviews.get(reviewId);
                if (review.timestamp != null) {
                    myReviews.add(review);
                }
            }
            myReviews.sort(Comparator.comparing(Review::getTimestamp).reversed());
            // Remove the duplicates reviews. We can only have 1 review per author per revision
            myReviews.removeIf(e -> !seen.add(e.author.id));
            return myReviews;
        }

        public List<RadDiscussion> getDiscussions() {
            if (discussion.comments == null || discussion.comments.isEmpty()) {
                return new ArrayList<>();
            }
            return discussion.comments.keySet().stream().map(discussion.comments::get)
                    .filter(c -> c != null && c.timestamp != null)
                    .collect(Collectors.toList());
        }

        public List<RadDiscussion> getReviewComments(String filePath) {
            return getDiscussions().stream().filter(disc -> disc != null && disc.isReviewComment() &&
                            disc.location.path.equals(filePath))
                    .collect(Collectors.toList());
        }

        public RadDiscussion findDiscussion(String commentId) {
            return getDiscussions().stream().filter(disc -> disc != null && disc.id.equals(commentId)).findFirst().orElse(null);
        }

        @JsonIgnore
        public String getDescription() {
            if (edits.isEmpty()) {
                return "";
            }
            var edit = edits.get(edits.size() - 1);
            return edit.body;
        }
    }

    public record Edit(RadAuthor author, String body, Instant timestamp, List<Embed> embeds) { }

    public record Merge(RadAuthor author, String commit, Instant timestamp, String revision) { }

    public record Review(String id, RadAuthor author, Verdict verdict, String summary,
                         DiscussionObj comments, Instant timestamp) implements TimelineEvent {
        @Override
        public Instant getTimestamp() {
            return timestamp == null ? Instant.now() : timestamp;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public enum Verdict {
            ACCEPT("--accept", "accept"),
            REJECT("--reject", "reject");

            private final String label;
            private final String value;

            Verdict(String value, String label) {
                this.value = value;
                this.label = label;
            }

            @JsonValue
            public String getValue() {
                return value;
            }

            @JsonCreator
            public static Verdict forValues(String val) {
                for (Verdict verdict : Verdict.values()) {
                    if (verdict.label.equals(val.toLowerCase())) {
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
