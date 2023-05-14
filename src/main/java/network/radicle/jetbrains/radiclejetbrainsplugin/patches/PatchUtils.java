package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatchUtils {
    private static final Logger logger = LoggerFactory.getLogger(PatchUtils.class);
    private final RadPatch patch;

    public PatchUtils(RadPatch patch) {
        this.patch = patch;
    }

    public Map<String, List<GitCommit>> calculateCommits() {
        var revisions = new HashMap<String, List<GitCommit>>();
        //TODO maybe here i have to add the did to the remotes before fetching
        var success = fetchCommits();
        if (!success) {
            return null;
        }
        try {
            for (var rev : patch.revisions) {
                var patchCommits = GitHistoryUtils.history(patch.repo.getProject(),
                        patch.repo.getRoot(), rev.base() + "..." + rev.oid());
                revisions.put(rev.id(), patchCommits);
            }
            return revisions;
        } catch (Exception e) {
            logger.warn("error calculating patch commits for patch: {}", patch, e);
            return null;
        }
    }

    private boolean fetchCommits() {
        var service = patch.repo.getProject().getService(RadicleProjectService.class);
        var output = service.fetchPeerChanges(patch);
        return RadAction.isSuccess(output);
    }

}
