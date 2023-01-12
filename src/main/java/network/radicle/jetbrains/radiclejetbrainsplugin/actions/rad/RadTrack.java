package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

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
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.track(repo, peer == null ? null : peer.id, node == null ? null : node.url);
    }

    public record Peer(String id) {
    }

    public record SeedNode(String url) {
    }
}
