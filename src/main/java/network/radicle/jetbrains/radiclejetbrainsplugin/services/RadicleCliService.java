package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadCobList;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadCobShow;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPatchCreate;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPatchLabel;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPatchReview;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadComment;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadIssueCreate;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadicleCliService {
    private static final Logger logger = LoggerFactory.getLogger(RadicleCliService.class);

    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    private final Project project;
    private final Map<String, RadProject> radRepoIds;
    private final RadicleProjectService rad;
    private RadDetails identity;

    public RadicleCliService(Project project) {
        this.project = project;
        radRepoIds = new HashMap<>();
        this.rad = project.getService(RadicleProjectService.class);
    }

    public ProcessOutput createIssue(GitRepository repo, String title, String description, List<String> assignees, List<String> labels) {
        var issueCreate = new RadIssueCreate(repo, title, description, assignees, labels);
        return issueCreate.perform();
    }

    public String createPatch(GitRepository repo, String title, String description, String branch, List<String> labels) {
        description = Strings.nullToEmpty(description).replace("\n", "<br/>");
        var radPatch = new RadPatchCreate(repo, title, description, branch);
        var output = radPatch.perform();
        if (!RadAction.isSuccess(output)) {
            return null;
        }
        var lines = output.getStderrLines();
        if (lines.isEmpty()) {
            return "";
        }
        var firstLine = lines.get(0);
        var parts = firstLine.split(" ");
        String patchId = null;
        if (parts.length > 2) {
            patchId = parts[2];
        }

        if (!labels.isEmpty() && !Strings.isNullOrEmpty(patchId)) {
            createPatchLabels(repo, patchId, labels, List.of());
        }
        return patchId;
    }

    public ProcessOutput createPatchLabels(GitRepository repo, String patchId,
                                           List<String> addedLabels, List<String> deletedLabels) {
        var radPatchLabel = new RadPatchLabel(repo, patchId, addedLabels, deletedLabels);
        return radPatchLabel.perform();
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
        issues.sort(Comparator.comparing(issue -> ((RadIssue) issue).discussion.get(0).timestamp).reversed());
        return issues;
    }

    public RadIssue issueCommentReact(RadIssue issue, String discussionId, String reaction, boolean active) {
        if (!active) {
            logger.error("not implemented yet from the CLI!");
            return null;
        }
        try {
            var res = rad.reactToIssueComment(issue.repo, issue.id, discussionId, reaction, active);
            if (!RadAction.isSuccess(res)) {
                logger.warn("received invalid command output:{} for reacting to issue:{} comment:{}. out:{} err:{}",
                        res.getExitCode(), issue.id, discussionId, res.getStdout(), res.getStderr());
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error reacting to discussion: {}", discussionId, e);
        }

        return null;
    }

    public ProcessOutput addReview(String message, String verdict, RadPatch patch) {
        var patchReview = new RadPatchReview(patch.repo, verdict, message, patch.getLatestRevision().id());
        return patchReview.perform();
    }

    public RadPatch getPatch(GitRepository repo, String projectId, String patchId) {
        var cobShow = new RadCobShow(repo, projectId, patchId, RadCobList.Type.PATCH);
        final var myIdentity = getCurrentIdentity();
        final var self = myIdentity == null ? null : myIdentity.toRadAuthor();
        var patch =  cobShow.getPatch();
        if (patch == null) {
            return null;
        }
        patch.id = patchId;
        patch.project = repo.getProject();
        patch.repo = repo;
        patch.self = self;
        patch.seedNode = getSeedNode();
        patch.radProject = getRadRepo(repo);
        return patch;
    }

    public  List<RadPatch> getPatches(GitRepository repo, String projectId) {
        var cobList = new RadCobList(repo, projectId, RadCobList.Type.PATCH);
        var listOutput = cobList.perform();
        var listOutputSuccess = RadAction.isSuccess(listOutput);
        if (!listOutputSuccess) {
            return List.of();
        }
        var patchesId = listOutput.getStdoutLines();
        var patches = new ArrayList<RadPatch>();
        for (var objectId : patchesId) {
            var patch = getPatch(repo, projectId, objectId);
            if (patch == null) {
                continue;
            }
            patches.add(patch);
        }
        patches.sort(Comparator.comparing(patch -> ((RadPatch) patch).getLatestRevision().getTimestamp()).reversed());
        return patches;
    }

    public RadDetails getCurrentIdentity() {
        if (identity != null) {
            return identity;
        }
        var radSelf = new RadSelf(project);
        radSelf.askForIdentity(false);
        identity = radSelf.getRadSelfDetails();
        return identity;
    }

    public void resetIdentity() {
        this.identity = null;
    }

    public ProcessOutput createPatchComment(GitRepository repo, String revisionId, String comment, String replyTo) {
        return createPatchComment(repo, revisionId, comment, replyTo, null, null);
    }

    public ProcessOutput createPatchComment(
            GitRepository repo, String revisionId, String comment, String replyTo, RadDiscussion.Location location, List<Embed> embedList) {
        //TODO: location and embeds are not supported by the CLI
        return createComment(repo, revisionId, comment, replyTo, RadComment.Type.PATCH);
    }

    public ProcessOutput createIssueComment(GitRepository repo, String issueId, String comment, String replyTo) {
        return createComment(repo, issueId, comment, replyTo, RadComment.Type.ISSUE);
    }

    public RadPatch changePatchTitleDescription(RadPatch patch, String newTitle, String newDescription) {
        try {
            var res = rad.editPatchTitleDescription(patch.repo, patch.id, newTitle, newDescription);
            if (!RadAction.isSuccess(res)) {
                logger.warn("received invalid command output for changing patch message (title/description): {} - {} - {}",
                        res.getExitCode(), res.getStdout(), res.getStderr());
                return null;
            }
            // return patch as-is, it will trigger a re-fetch anyway
            return patch;
        } catch (Exception e) {
            logger.warn("error changing patch message (title/description): {}", patch, e);
        }

        return null;
    }

    public RadIssue changeIssueTitleDescription(RadIssue issue, String newTitle, String newDescription) {
        try {
            var res = rad.editIssueTitleDescription(issue.repo, issue.id, newTitle, newDescription);
            if (!RadAction.isSuccess(res)) {
                logger.warn("received invalid command output for changing issue message (title/description): {} - {} - {}",
                        res.getExitCode(), res.getStdout(), res.getStderr());
                return null;
            }
            // return issue as-is, it will trigger a re-fetch anyway
            return issue;
        } catch (Exception e) {
            logger.warn("error changing issue message (title/description): {}", issue, e);
        }

        return null;
    }

    public RadPatch changePatchState(RadPatch patch, String state) {
        try {
            var res = rad.changePatchState(patch.repo, patch.id, patch.state.status, state);
            if (!RadAction.isSuccess(res)) {
                logger.warn("received invalid command output:{} for changing patch state: {} - out:{} - err:{}",
                        res.getExitCode(), res.getStdout(), res.getStderr());
                return null;
            }
            // return issue as-is, it will trigger a re-fetch anyway
            return patch;
        } catch (Exception e) {
            logger.warn("error changing state to patch: {}", patch, e);
        }
        return null;
    }

    public RadProject getRadRepo(GitRepository repo) {
        RadProject radRepo;
        if (radRepoIds.containsKey(repo.getRoot().getPath())) {
            radRepo = radRepoIds.get(repo.getRoot().getPath());
        } else {
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

    protected SeedNode getSeedNode() {
        var sh = new RadicleProjectSettingsHandler(project);
        var settings = sh.loadSettings();
        return settings.getSeedNode();
    }

    public Project getProject() {
        return project;
    }

    private ProcessOutput createComment(GitRepository repo, String id, String comment, String replyTo, RadComment.Type type) {
        final RadComment radComment;
        if (!Strings.isNullOrEmpty(replyTo)) {
            radComment = new RadComment(repo, id, replyTo, comment, type);
        } else {
            radComment = new RadComment(repo, id, comment, type);
        }
        return radComment.perform();
    }

    public String getWebUrl() {
        final var defaultWebNode = "seed.radicle.garden";
        final var radHome = new RadicleProjectSettingsHandler(project).loadSettings().getRadHome();
        if (Strings.isNullOrEmpty(radHome) || !Files.exists(Paths.get(radHome))) {
            return defaultWebNode;
        }
        try {
            var configJson = Files.readString(Paths.get(radHome, "config.json"));
            var conf = MAPPER.readTree(configJson);
            if (conf.has("preferredSeeds")) {
                var pref = conf.get("preferredSeeds");
                if (pref.isArray() && !pref.isEmpty()) {
                    var prefSeed = pref.get(0).asText();
                    if (!Strings.isNullOrEmpty(prefSeed)) {
                        prefSeed = prefSeed.split("@")[1].split(":")[0];
                        return prefSeed;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("error reading config.json", e);
        }

        return defaultWebNode;
    }

    public String getPatchWebUrl(RadPatch radPatch) {
        return createPatchWebUrl(radPatch, getWebUrl());
    }

    public String getIssueWebUrl(RadIssue radIssue) {
        return createIssueWebUrl(radIssue, getWebUrl());
    }

    protected String createPatchWebUrl(RadPatch patch, String node) {
        return "https://app.radicle.xyz/nodes/" + node + "/" + patch.radProject.id + "/patches/" + patch.id;
    }

    protected String createIssueWebUrl(RadIssue issue, String node) {
        return "https://app.radicle.xyz/nodes/" + node + "/" + issue.projectId + "/issues/" + issue.id;
    }
}
