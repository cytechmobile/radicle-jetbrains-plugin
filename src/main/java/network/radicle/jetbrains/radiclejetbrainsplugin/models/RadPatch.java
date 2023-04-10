package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import git4idea.repo.GitRepository;

import java.time.Instant;
import java.util.List;

public class RadPatch {
    public GitRepository repo;
    public String patchId;
    public String author;
    public String title;
    public String description;
    public State state;
    public String target;
    public List<Revision> revisions;

    public RadPatch(GitRepository repo, String patchId, String author, String title, String description, State state, String target,
                    List<Revision> revisions) {
        this.repo = repo;
        this.patchId = patchId;
        this.author = author;
        this.title = title;
        this.description = description;
        this.state = state;
        this.target = target;
        this.revisions = revisions;
    }

    public enum State {
        OPEN, CLOSED, MERGED
    }

    public record Revision(String patchId, String description, String base, String commitHash, Instant created, List<String> refs,
                           List<Comment> comments, List<Merge> merges) {
    }

    public record Comment(String id, String author, String body, Instant created, String replyTo) {

    }

    public record Merge(String node, String commit, Instant timeStamp) {
    }

}
