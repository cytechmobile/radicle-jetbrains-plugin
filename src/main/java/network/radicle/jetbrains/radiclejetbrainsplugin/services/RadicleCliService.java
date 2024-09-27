package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadCobList;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadCobShow;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadicleCliService {
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    private static final Logger logger = LoggerFactory.getLogger(RadicleCliService.class);

    private final RadicleProjectSettingsHandler projectSettingsHandler;
    private final Project project;
    private final Map<String, RadProject> radRepoIds;
    private RadDetails identity;

    public RadicleCliService(Project project) {
        this.projectSettingsHandler = new RadicleProjectSettingsHandler(project);
        this.project = project;
        radRepoIds = new HashMap<>();
    }

    public RadDetails getCurrentIdentity() {
        if (identity != null) {
            return identity;
        }
        var self = new RadSelf(project);
        this.identity = self.getRadSelfDetails();
        return identity;
    }

    public void resetIdentity() {
        this.identity = null;
    }

    public RadProject getRadRepo(GitRepository repo) {
        RadProject radRepo;
        if (radRepoIds.containsKey(repo.getRoot().getPath())) {
            radRepo = radRepoIds.get(repo.getRoot().getPath());
        } else {
            var rad = project.getService(RadicleProjectService.class);
            var output = rad.inspect(repo);
            if (!RadAction.isSuccess(output)) {
                RadAction.showErrorNotification(project, RadicleBundle.message("radCliError"), RadicleBundle.message("errorFindingProjectId"));
                return null;
            }
            var radProjectId = output.getStdout().trim();
            output = rad.inspectIdentity(repo);
            if (!RadAction.isSuccess(output)) {
                RadAction.showErrorNotification(project, RadicleBundle.message("radCliError"), RadicleBundle.message("errorFindingProjectId"));
                return null;
            }
            final var json = output.getStdout().trim();
            String defaultBranch;
            String description;
            String name;
            try {
                var identityJson = MAPPER.readTree(json);
                var payload = identityJson.get("payload");
                var proj = payload.get("xyz.radicle.project");
                defaultBranch = proj.get("defaultBranch").asText();
                description = proj.get("description").asText();
                name = proj.get("name").asText();
            } catch (Exception e) {
                logger.warn("Error parsing identity json for repo:{}", radProjectId, e);
                return null;
            }

            output = rad.inspectDelegates(repo);
            if (!RadAction.isSuccess(output)) {
                RadAction.showErrorNotification(project, RadicleBundle.message("radCliError"), RadicleBundle.message("errorFindingProjectId"));
                return null;
            }
            final var delegatesArr = output.getStdout().trim().split("\n");
            List<RadAuthor> delegates = new ArrayList<>();
            for (var delegate : delegatesArr) {
                var parts = delegate.split(" ");
                var key = parts[0].replace("did:key:", "");
                String alias = null;
                if (parts.length > 1) {
                    alias = parts[1];
                    if (alias.startsWith("(")) {
                        alias = alias.substring(1);
                    }
                    if (alias.endsWith(")")) {
                        alias = alias.substring(0, alias.length() - 1);
                    }
                }
                delegates.add(new RadAuthor(key, alias));
            }

            radRepo = new RadProject(radProjectId, name, description, defaultBranch, delegates);
            radRepoIds.put(repo.getRoot().getPath(), radRepo);
        }
        // find where latest default branch points to
        GitRemote radRemote = null;
        for (var remote : repo.getRemotes()) {
            for (var url : remote.getUrls()) {
                if (url.startsWith("rad://")) {
                    radRemote = remote;
                    break;
                }
            }
            if (radRemote != null) {
                break;
            }
        }
        if (radRemote == null) {
            RadAction.showErrorNotification(project, RadicleBundle.message("radCliError"), RadicleBundle.message("errorFindingProjectId"));
            return null;
        }

        var gitResult = Git.getInstance().lsRemoteRefs(project, repo.getRoot(), radRemote, List.of(radRepo.defaultBranch));
        if (gitResult != null && gitResult.getOutput() != null && !gitResult.getOutput().isEmpty()) {
            var gitOutput = gitResult.getOutput().get(0);
            radRepo.head = gitOutput.split(" ")[0].split("\t")[0];
        }
        return radRepo;
    }

    public RadIssue getIssue(GitRepository repo, String projectId, String objectId) {
        var cobShow = new RadCobShow(repo, projectId, objectId, RadCobList.Type.ISSUE);
        return cobShow.getIssue();
    }

    public List<RadIssue> getIssues(GitRepository repo, String projectId) {
        var cobList = new RadCobList(repo, projectId, RadCobList.Type.ISSUE);
        var listOutput = cobList.perform();
        var listOutputSuccess = RadAction.isSuccess(listOutput);
        if (!listOutputSuccess) {
            return List.of();
        }
        var issueIds = listOutput.getStdoutLines();
        var issues = new ArrayList<RadIssue>();
        for (var objectId : issueIds) {
            var issue = getIssue(repo, projectId, objectId);
            if (issue == null) {
                continue;
            }
            issues.add(issue);
        }
        return issues;
    }
}
