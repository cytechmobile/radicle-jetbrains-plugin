package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
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
import java.util.stream.Collectors;
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
        for (var revision : revisions) {
            var found = revision.discussions.stream().anyMatch(d -> d.id.equals(commentId));
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
            String id, RadAuthor author,
            @JsonIgnore
            String description,
            @JsonProperty("description")
            List<Edit> edits,
            List<Reaction> reactions, String base, String oid, List<String> refs,
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

        public List<RadDiscussion> getReviewComments(String filePath) {
            return discussions.stream().filter(disc -> disc.isReviewComment() &&
                            disc.location.path.equals(filePath))
                    .collect(Collectors.toList());
        }

        public RadDiscussion findDiscussion(String commentId) {
            return discussions().stream().filter(disc -> disc.id.equals(commentId)).findFirst().orElse(null);
        }
    }

    public record Edit(RadAuthor author, String body, Instant timestamp, List<Embed> embeds) {
    }

    public record Merge(RadAuthor author, String commit, Instant timestamp, String revision) {
    }

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

    public static RadPatchCli getPatchCli(RadPatch other) {
        var radPatchCli = new RadPatchCli();
        radPatchCli.id = other.id;
        radPatchCli.title = other.title;
        radPatchCli.author = other.author;
        radPatchCli.labels = other.labels;
        radPatchCli.state = other.state;
        var map = new HashMap<String, RadPatchCli.Revision>();
        for (var revision : other.revisions) {
            var reviewMap = new HashMap<String, List<RadPatchCli.Review>>();
            for (var review : revision.reviews) {
                var exists = reviewMap.get(review.author.id);
                if (exists == null) {
                    var discussionMap = new HashMap<String, RadPatchCli.MyDisc>();
                    for (var disc : review.comments) {
                        var newDisc = new RadPatchCli.MyDisc(disc.author, disc.reactions, disc.embeds,
                                disc.replyTo, disc.body, disc.location, List.of(new RadPatchCli.Edits(disc.author.id, disc.timestamp, disc.body, disc.embeds)));
                        discussionMap.put(disc.id, newDisc);
                    }
                    reviewMap.put(review.author.id, List.of(new RadPatchCli.Review(review.id, review.author,
                            review.verdict, review.summary, new RadPatchCli.MyDiscussion(discussionMap, List.of()), review.timestamp)));
                } else {
                    var myList = reviewMap.get(review.author.id);
                    var discussionMap = new HashMap<String, RadPatchCli.MyDisc>();
                    for (var disc : review.comments) {
                        var newDisc = new RadPatchCli.MyDisc(disc.author, disc.reactions, disc.embeds,
                                disc.replyTo, disc.body, disc.location, List.of(new RadPatchCli.Edits(disc.author.id, disc.timestamp, disc.body, disc.embeds)));
                        discussionMap.put(disc.id, newDisc);
                    }
                    myList.add(new RadPatchCli.Review(review.id, review.author,
                            review.verdict, review.summary, new RadPatchCli.MyDiscussion(discussionMap, List.of()), review.timestamp));
                }
            }
            var discussionMap = new HashMap<String, RadPatchCli.MyDisc>();
            for (var disc : revision.discussions) {
                var newDisc = new RadPatchCli.MyDisc(disc.author, disc.reactions, disc.embeds,
                        disc.replyTo, disc.body, disc.location, List.of(new RadPatchCli.Edits(disc.author.id, disc.timestamp, disc.body, disc.embeds)));
                discussionMap.put(disc.id, newDisc);
            }
            var patchCliRevision = new RadPatchCli.Revision(revision.id,
                    revision.author, revision.edits, revision.reactions, revision.base, revision.oid,
                    revision.refs, reviewMap, revision.timestamp, new RadPatchCli.MyDiscussion(discussionMap, List.of()));
            map.put(revision.id, patchCliRevision);
            radPatchCli.revisions = map;
        }
        return radPatchCli;
    }

    public static class RadPatchCli {
        public String id;
        public String title;
        public RadAuthor author;
        public List<String> labels;
        public State state;
        public String target;
        public Map<String, Revision> revisions;

        public record Revision(
                String id, RadAuthor author,
                @JsonProperty("description")
                List<Edit> edits,
                List<Reaction> reactions, String base, String oid, List<String> refs,
                Map<String, List<Review>> reviews,
                Instant timestamp, MyDiscussion discussion) {
        }

        public record MyDiscussion(Map<String, MyDisc> comments, List<String> timeline) {
        }

        public record Edits(String author, Instant timestamp, String body, List<Embed> embeds) {
        }

        public record MyDisc(RadAuthor author,
                             @JsonDeserialize(using = RadDiscussion.ReactionDeserializer.class)
                             List<Reaction> reactions,
                             List<Embed> embeds,
                             String replyTo,
                             String body, RadDiscussion.Location location, List<Edits> edits) {
        }

        public record Review(String id, RadAuthor author, RadPatch.Review.Verdict verdict, String summary,
                             MyDiscussion comments, Instant timestamp) implements TimelineEvent {
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
                public static RadPatch.Review.Verdict forValues(String val) {
                    for (RadPatch.Review.Verdict verdict : RadPatch.Review.Verdict.values()) {
                        if (verdict.value.equals(val.toLowerCase())) {
                            return verdict;
                        }
                    }
                    return null;
                }
            }
        }
    }

    public static RadPatch deserialize(String json) {
        try {
            var patchCli = RadicleCliService.MAPPER.readValue(json, new TypeReference<RadPatchCli>() {
            });
            var revisions = new ArrayList<Revision>();
            for (var key : patchCli.revisions.keySet()) {
                var rev = patchCli.revisions.get(key);
                var discussions = new ArrayList<RadDiscussion>();
                var reviews = new ArrayList<Review>();

                for (var discId : rev.discussion.comments.keySet()) {
                    var disc = rev.discussion.comments.get(discId);
                    if (disc == null) {
                        continue;
                    }
                    var discussion = new RadDiscussion(discId, disc.author,
                            disc.body, !disc.edits.isEmpty() ? disc.edits.get(0).timestamp : Instant.now(), disc.replyTo, disc.reactions,
                            !disc.edits.isEmpty() ? disc.edits.get(0).embeds : List.of(), disc.location);
                    discussions.add(discussion);
                }

                for (var key1 : rev.reviews.keySet()) {
                    var myReviews = rev.reviews.get(key1);
                    var myDiscussions = new ArrayList<RadDiscussion>();
                    for (var myReview : myReviews) {
                        for (var discId : myReview.comments.comments.keySet()) {
                            var disc = myReview.comments.comments.get(discId);
                            if (disc == null) {
                                continue;
                            }
                            var discussion = new RadDiscussion(discId, disc.author,
                                    disc.body, !disc.edits.isEmpty() ? disc.edits.get(0).timestamp : Instant.now(), disc.replyTo,
                                    disc.reactions, !disc.edits.isEmpty() ? disc.edits.get(0).embeds : List.of(), disc.location);
                            myDiscussions.add(discussion);
                        }
                        var myR = new Review(myReview.id, myReview.author, myReview.verdict, myReview.summary, myDiscussions, myReview.timestamp);
                        reviews.add(myR);
                    }
                }
                discussions.sort(Comparator.comparing(RadDiscussion::getTimestamp));
                var myRevision = new Revision(rev.id, rev.author,
                        !rev.edits.isEmpty() ? rev.edits.get(0).body : "", rev.edits,
                        rev.reactions, rev.base, rev.oid, List.of(), rev.timestamp, discussions, reviews);
                revisions.add(myRevision);
            }
            revisions.sort(Comparator.comparing(Revision::getTimestamp));
            return new RadPatch("", null, null, patchCli.title, patchCli.author,
                    patchCli.target, patchCli.labels, patchCli.state, revisions);
        } catch (Exception e) {
            logger.warn("Unable to deserialize patch");
        }
        return null;
    }

}
