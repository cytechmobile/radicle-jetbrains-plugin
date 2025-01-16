package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;

public class RadComment extends RadAction {
    private String replyTo;
    private final String id;
    private final String comment;
    private final Type type;

    public RadComment(GitRepository repo, String id, String replyTo, String comment, Type type) {
        super(repo);
        this.id = id;
        this.replyTo = replyTo;
        this.comment = comment;
        this.type = type;
    }

    public RadComment(GitRepository repo, String id, String comment, Type type) {
        super(repo);
        this.id = id;
        this.type = type;
        this.comment = comment;
    }

    @Override
    public ProcessOutput run() {
        return rad.comment(repo, id, comment, replyTo, type);
    }

    @Override
    public String getActionName() {
        return "commentError";
    }

    public enum Type {
        ISSUE("issue"),
        PATCH("patch");
        public final String value;
        Type(String value) {
            this.value = value;
        }
    }
}
