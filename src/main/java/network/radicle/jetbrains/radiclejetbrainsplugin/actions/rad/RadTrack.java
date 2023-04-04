package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadTrack extends RadAction {
    protected Peer peer;
    protected SeedNode node;

    public RadTrack(GitRepository repo, Peer peer) {
        this(repo, peer, null);
    }

    public RadTrack(GitRepository repo, SeedNode node) {
        this(repo, null, node);
    }

    public RadTrack(GitRepository repo, Peer peer, SeedNode node) {
        super(repo);
        this.peer = peer;
        this.node = node;
    }

    @Override
    public String getActionName() {
        return "Track";
    }

    @Override
    public ProcessOutput run() {
        var rad = repo.getProject().getService(RadicleProjectService.class);
        return rad.track(repo, peer == null ? null : peer.id, node == null ? null : node.url);
    }

    public record Peer(String id) {
    }

    public record SeedNode(String url) {
    }
}
