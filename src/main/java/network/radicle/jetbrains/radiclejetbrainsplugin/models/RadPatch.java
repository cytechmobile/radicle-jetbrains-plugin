package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import git4idea.repo.GitRepository;

public class RadPatch {
    public String peerId;
    public String author;
    public GitRepository repo;

    public RadPatch(String peerId,String author, GitRepository repo) {
        this.peerId = peerId;
        this.repo = repo;
        this.author = author;
    }
}
