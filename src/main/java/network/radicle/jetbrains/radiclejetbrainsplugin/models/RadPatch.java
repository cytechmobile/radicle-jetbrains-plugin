package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import git4idea.repo.GitRepository;

public class RadPatch {
    public String peerId;
    public GitRepository repo;

    public RadPatch(String peerId, GitRepository repo) {
        this.peerId = peerId;
        this.repo = repo;
    }
}
