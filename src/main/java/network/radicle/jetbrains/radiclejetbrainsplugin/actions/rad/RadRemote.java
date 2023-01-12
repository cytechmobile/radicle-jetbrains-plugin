package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

import java.util.ArrayList;
import java.util.List;

public class RadRemote extends RadAction {
    public RadRemote(GitRepository repo) {
        super(repo);
    }

    @Override
    public String getActionName() {
        return "Remote";
    }

    @Override
    public ProcessOutput run() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.remoteList(repo);
    }

    public List<Peer> findTrackedPeers() {
        var out = this.perform();
        var peers = new ArrayList<Peer>();
        var lines = out.getStdoutLines();
        for (var line : lines) {
            var firstSpaceIdx = line.indexOf(' ');
            var name = line.substring(0, firstSpaceIdx);
            var id = line.substring(firstSpaceIdx + 1).split(" ")[0].trim();
            peers.add(new Peer(name.trim(), id.trim()));
        }
        return peers;
    }

    public record Peer(String name, String id) { }
}
