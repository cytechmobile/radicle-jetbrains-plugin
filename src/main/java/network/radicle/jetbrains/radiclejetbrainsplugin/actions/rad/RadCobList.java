package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadCobList extends RadAction {
    private final String projectId;
    private final Type type;

    public RadCobList(GitRepository repo, String projectId, Type type) {
        super(repo);
        this.projectId = projectId;
        this.type = type;
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.cobList(repo, projectId, type);
    }

    @Override
    public String getActionName() {
        return null;
    }

    public enum Type {
        ISSUE("xyz.radicle.issue"),
        PATCH("xyz.radicle.patch");

        public final String value;

        Type(String value) {
            this.value = value;
        }
    }
}
