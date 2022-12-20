package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import com.intellij.openapi.vcs.changes.Change;
import git4idea.repo.GitRepository;

import java.util.Collection;
import java.util.List;

public class RadPatch {
    public GitRepository repo;
    public String peerId;
    public String author;
    public boolean self;
    public String branchName;
    public String commitHash;

    public List<Change> changes;

    public RadPatch(GitRepository repo,String author, String peerId, boolean self, String branchName, String commitHash) {
        this.repo = repo;
        this.peerId = peerId;
        this.self = self;
        this.branchName = branchName;
        this.commitHash = commitHash;
        this.author = author;
    }

    @Override
    public String toString() {
        return "RadPatch{" +
                "repo=" + repo +
                ", peerId='" + peerId + '\'' +
                ", self=" + self +
                ", branchName='" + branchName + '\'' +
                ", commitHash='" + commitHash + '\'' +
                '}';
    }
}
