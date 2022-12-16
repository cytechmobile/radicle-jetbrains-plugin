package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import git4idea.repo.GitRepository;

public class RadPatch {
    public GitRepository repo;
    public String peerId;
    public boolean self;
    public String branchName;
    public String commitHash;

    public RadPatch(GitRepository repo, String peerId, boolean self, String branchName, String commitHash) {
        this.repo = repo;
        this.peerId = peerId;
        this.self = self;
        this.branchName = branchName;
        this.commitHash = commitHash;
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
