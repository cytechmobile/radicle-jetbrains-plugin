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
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadComment;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadIssueCreate;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPatchCreate;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPatchLabel;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPatchReview;
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
import java.time.Instant;
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
    protected final Project project;
    protected final Map<String, RadProject> radRepoIds;
    protected RadicleProjectService rad;
    protected RadicleNativeService jrad;
    protected RadDetails identity;

    public RadicleCliService(Project project) {
        this.project = project;
        radRepoIds = new HashMap<>();
        this.rad = project.getService(RadicleProjectService.class);
        this.jrad = project.getService(RadicleNativeService.class);
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
        var firstLine = lines.getFirst();
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

    public ProcessOutput createPatchLabels(GitRepository repo, String patchId, List<String> addedLabels, List<String> deletedLabels) {
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
        List<RadIssue> issues = new ArrayList<>();
        for (var objectId : issueIds) {
            var issue = getIssue(repo, projectId, objectId);
            if (issue == null) {
                continue;
            }

            issues.add(issue);
        }
        issues.sort(Comparator.comparing((RadIssue issue) -> issue.discussion == null || issue.discussion.isEmpty() ? Instant.now() :
                issue.discussion.getFirst().timestamp).reversed());
        return issues;
    }

    public RadIssue editIssueComment(RadIssue issue, String commentId, String comment, List<Embed> embeds) {
        boolean ok = jrad.editIssueComment(issue.projectId, issue.id, commentId, comment, embeds);
        return ok ? issue : null;
    }

    public RadPatch patchCommentReact(RadPatch patch, String commentId, String reaction, boolean active) {
        try {
            var revId = patch.findRevisionId(commentId);
            var res = jrad.patchCommentReact(patch.radProject.id, patch.id, revId, commentId, reaction, active);
            if (!res) {
                logger.warn("received invalid result for reacting to patch:{} comment:{}", patch.id, commentId);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error reacting to patch:{} comment: {}", patch.id, commentId, e);
        }

        return null;
    }

    public RadIssue issueCommentReact(RadIssue issue, String commentId, String reaction, boolean active) {
        if (!active) {
            boolean ok = jrad.issueCommentReact(issue.projectId, issue.id, commentId, reaction, false);
            return ok ? issue : null;
        }
        try {
            var res = rad.reactToIssueComment(issue.repo, issue.id, commentId, reaction, true);
            if (!RadAction.isSuccess(res)) {
                logger.warn("received invalid command output:{} for reacting to issue:{} comment:{}. out:{} err:{}",
                        res.getExitCode(), issue.id, commentId, res.getStdout(), res.getStderr());
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error reacting to discussion: {}", commentId, e);
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

    public String getAlias(String did) {
        return jrad.getAlias(did);
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

    public boolean createPatchComment(
            RadPatch patch, String revisionId, String comment, String replyTo, RadDiscussion.Location location, List<Embed> embedList) {
        if (location == null && (embedList == null || embedList.isEmpty())) {
            var res = createComment(patch.repo, revisionId, comment, replyTo, RadComment.Type.PATCH);
            return RadAction.isSuccess(res);
        }
        return jrad.createPatchComment(patch.radProject.id, patch.id, revisionId, comment, replyTo, location, embedList);
    }

    public RadPatch editPatchComment(
            RadPatch patch, String revisionId, String commentId, String comment, List<Embed> embedList) {
        boolean ok = jrad.editPatchComment(patch.radProject.id, patch.id, revisionId, commentId, comment, embedList);
        return ok ? patch : null;
    }

    public RadPatch deletePatchComment(RadPatch patch, String revisionId, String commentId) {
        boolean ok = jrad.deletePatchComment(patch.radProject.id, patch.id, revisionId, commentId);
        return ok ? patch : null;
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

    public RadIssue changeIssueTitleDescription(RadIssue issue, String newTitle, String newDescription, List<Embed> embeds) {
        try {
            var resp = jrad.editIssueTitleDescription(issue.projectId, issue.id, newTitle, newDescription, embeds);
            if (!resp.ok()) {
                logger.warn("received invalid native response for changing issue(title/description): repoId:{} issueId:{} title:{} description:{} resp:{}",
                        issue.projectId, issue.id, newTitle, newDescription, resp);
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
                        res.getExitCode(), patch.id, res.getStdout(), res.getStderr());
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

        try {
            var gitResult = Git.getInstance().lsRemoteRefs(project, repo.getRoot(), radRemote, List.of(radRepo.defaultBranch));
            if (!gitResult.getOutput().isEmpty()) {
                var gitOutput = gitResult.getOutput().getFirst();
                radRepo.head = gitOutput.split(" ")[0].split("\t")[0];
            }
        } catch (Exception e) {
            logger.warn("error getting head for repo:{}", repo.getRoot().getPath(), e);
            return radRepo;
        }
        return radRepo;
    }

    public Map<String, String> getEmbeds(String repoId, List<String> oids) {
        try {
            return jrad.getEmbeds(repoId, oids);
        } catch (Exception e) {
            logger.warn("error getting embeds for repo:{} oids:{}", repoId, oids, e);
            return new HashMap<>();
        }
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
