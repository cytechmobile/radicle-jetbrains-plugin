package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;

public class RadTrack extends RadAction {
    protected RadTrackType type;
    protected Peer peer;
    protected Repo repo;

    public RadTrack(Project project, Repo repository) {
        super(project);
        this.type = RadTrackType.TRACK_REPOSITORY;
        repo = repository;
    }

    public RadTrack(Project project, Peer peer) {
        super(project);
        this.type = RadTrackType.TRACK_PEER;
        this.peer = peer;
    }

    @Override
    public String getActionName() {
        return "Track";
    }

    @Override
    public ProcessOutput run() {
        if (type == RadTrackType.TRACK_PEER) {
            return rad.trackPeer(peer);
        } else {
            return rad.trackRepo(repo);
        }
    }

    public record Peer(String nid, String alias) {
    }

    public record Repo(String rid, Scope scope) {
    }

    public enum Scope {
        NONE(RadicleBundle.message("none"), "none"),
        TRUSTED(RadicleBundle.message("trusted"), "trusted"),
        ALL(RadicleBundle.message("all"), "all");

        public final String label;
        public final String value;
        Scope(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    public enum RadTrackType {
        TRACK_REPOSITORY(RadicleBundle.message("trackProject")),
        TRACK_PEER(RadicleBundle.message("trackPeer"));

        public final String name;
        RadTrackType(String name) {
            this.name = name;
        }
    }
}
