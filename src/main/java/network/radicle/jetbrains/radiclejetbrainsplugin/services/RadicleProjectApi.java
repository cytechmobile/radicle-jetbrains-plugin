package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.serviceContainer.NonInjectable;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
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
import network.radicle.jetbrains.radiclejetbrainsplugin.services.auth.AuthService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction.showNotification;

public class RadicleProjectApi {
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    public static final int ALL_IN_ONE_PAGE = 10000;
    public static final int TIMEOUT = 5000;
    private static final Logger logger = LoggerFactory.getLogger(RadicleProjectApi.class);
    private static final int PER_PAGE = 10;

    private final CloseableHttpClient client;
    private final Project project;
    protected Cache<String, Session> sessions;
    protected RadDetails identity;

    protected final Map<String, RadAuthor> aliases;

    public RadicleProjectApi(Project project) {
        this(project, HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(TIMEOUT) // Socket timeout in milliseconds
                .setConnectTimeout(TIMEOUT) // Connection timeout in milliseconds
                .build()).build());
    }

    @NonInjectable
    public RadicleProjectApi(Project project, CloseableHttpClient client) {
        this.project = project;
        this.client = client;
        this.sessions = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();
        aliases = new HashMap<>();
    }

    public SeedNodeInfo checkApi(SeedNode node) {
        var url = node.url + "/api/v1";
        try {
            var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchSeedNodeError"));
            if (res.isSuccess()) {
                var json = new ObjectMapper().readTree(res.body);
                String version = json.get("version").asText("");
                String id = json.get("nid").asText("");
                return new SeedNodeInfo(id, version, null);
            }
            return new SeedNodeInfo(null, null, "HTTP Status code: " + res.status + " " + res.body);
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return new SeedNodeInfo(null, null, "Exception: " + e.getMessage());
        }
    }

    public RadIssue editIssueComment(RadIssue issue, String comment, String id, List<Embed> embedList) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "comment.edit", "id", id, "body", comment, "replyTo", issue.id, "embeds", embedList);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("commentEditError"), RadicleBundle.message("commentDescError"));
            if (!resp.isSuccess()) {
                logger.warn("error editing comment: {} to issue:{} resp:{}", comment, issue, resp);
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error editing issue comment: {}", issue, e);
        }
        return null;
    }

    public List<RadIssue> fetchIssues(String projectId, GitRepository repo) {
        var node = getSeedNode();
        var allIssues = new ArrayList<RadIssue>();
        var states = Arrays.stream(RadIssue.State.values()).map(e -> e.status).toList();
        try {
            for (var state : states) {
                var url = node.url + "/api/v1/projects/" + projectId + "/issues?page=0&state=" + state + "&perPage=" + ALL_IN_ONE_PAGE;
                var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchIssuesError"));
                if (res.status == -1) {
                    break;
                }
                if (res.isSuccess()) {
                    var issues = MAPPER.readValue(res.body, new TypeReference<List<RadIssue>>() { });
                    for (var issue : issues) {
                        issue.repo = repo;
                        issue.projectId = projectId;
                        issue.project = repo.getProject();
                        issue.seedNode = node;
                    }
                    allIssues.addAll(issues);
                }
            }
            return allIssues;
        } catch (Exception e) {
            logger.warn("http request exception", e);
        }
        return allIssues;
    }

    public List<RadPatch> fetchPatches(String projectId, GitRepository repo) {
        final var radProject = fetchRadProject(projectId);
        if (radProject == null) {
            return List.of();
        }
        final var myIdentity = getCurrentIdentity();
        var self = myIdentity == null ? null : myIdentity.toRadAuthor();
        var node = getSeedNode();
        var states = Arrays.stream(RadPatch.State.values()).map(e -> e.status).toList();
        var allPatches = new ArrayList<RadPatch>();
        try {
            for (var state : states) {
                var url = node.url + "/api/v1/projects/" + projectId + "/patches?page=0&state=" + state + "&perPage=" + ALL_IN_ONE_PAGE;
                var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchPatchesError"));
                if (res.status == -1) {
                    break;
                }
                if (res.isSuccess()) {
                    var patches = MAPPER.readValue(res.body, new TypeReference<List<RadPatch>>() { });
                    for (var patch : patches) {
                        patch.seedNode = node;
                        patch.project = repo.getProject();
                        patch.radProject = radProject;
                        patch.self = self;
                        patch.repo = repo;
                    }
                    allPatches.addAll(patches);
                }
            }
            return allPatches;
        } catch (Exception e) {
            logger.warn("http request exception", e);
        }
        return List.of();
    }

    public RadPatch addReview(String verdict, String summary, RadPatch patch) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.radProject.id + "/patches/" + patch.id);
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchEditData = Map.of("type", "review", "revision", patch.getLatestRevision().id(), "summary", summary, "verdict", verdict,
                    "labels", List.of());
            var json = MAPPER.writeValueAsString(patchEditData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(patchReq, RadicleBundle.message("reviewTitleError"));
            if (!resp.isSuccess()) {
                logger.warn("received invalid response with status:{} and body:{} while adding a review: {}",
                        resp.status, resp.body, patch);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error adding patch review: {}", patch, e);
        }

        return null;
    }

    public RadPatch fetchPatch(String projectId, GitRepository repo, String patchId) {
        final var radProject = fetchRadProject(projectId);
        if (radProject == null) {
            return null;
        }
        final var myIdentity = getCurrentIdentity();
        final var self = myIdentity == null ? null : myIdentity.toRadAuthor();
        final var node = getSeedNode();
        final var url = node.url + "/api/v1/projects/" + projectId + "/patches/" + patchId;
        try {
            var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchPatchError"));
            if (res.isSuccess()) {
                var patch = MAPPER.readValue(res.body, RadPatch.class);
                patch.seedNode = node;
                patch.project = repo.getProject();
                patch.radProject = radProject;
                patch.self = self;
                patch.repo = repo;
                return patch;
            }
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
        }
        return null;
    }

    public List<RadProject> fetchRadProjects(int page) {
        var url = getHttpNodeUrl() + "/api/v1/projects?show=all&perPage=" + PER_PAGE + "&page=" + page;
        try {
            var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchProjectsError"));
            if (res.isSuccess()) {
                if (Strings.isNullOrEmpty(res.body)) {
                    return new ArrayList<>();
                }
                return MAPPER.readValue(res.body, new TypeReference<>() { });
            }
            return null;
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return null;
        }
    }

    public RadProject fetchRadProject(String projectId) {
        var url = getHttpNodeUrl() + "/api/v1/projects/" + projectId;
        try {
            var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchProjectError"));
            if (res.isSuccess()) {
                return MAPPER.readValue(res.body, RadProject.class);
            }
            return null;
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            showNotification(project, RadicleBundle.message("fetchProjectError"), "", NotificationType.ERROR, null);
            return null;
        }
    }

    public boolean createIssue(String title, String description, List<String> assignees,
                               List<String> labels, GitRepository repo, String projectId, List<Embed> embedList) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return false;
        }
        try {
            var issueReq = new HttpPost(getHttpNodeUrl() + "/api/v1/projects/" + projectId + "/issues");
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("title", title, "description", description, "labels", labels, "assignees", assignees, "embeds", embedList);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("createIssueError"), RadicleBundle.message("commentDescError"));
            if (!resp.isSuccess()) {
                logger.warn("error creating new issue, title : {}, assignees : {}, labels : {}, " +
                        "repo : {}, projectId : {}", title, assignees, labels, repo, projectId);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.warn("error creating new issue, title : {}, assignees : {}, labels : {}, " +
                    "repo : {}, projectId : {}", title, assignees, labels, repo, projectId);
        }
        return false;
    }

    public RadPatch changePatchState(RadPatch patch, String state) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.radProject.id + "/patches/" + patch.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "lifecycle", "state", Map.of("status", state, "reason", "other"));
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("stateChangeError"));
            if (!resp.isSuccess()) {
                logger.warn("error changing state {} to patch:{} resp:{}", state, patch, resp);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error changing state to patch: {}", patch, e);
        }
        return null;
    }

    public RadPatch addRemovePatchLabel(RadPatch patch, List<String> addLabelList) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.radProject.id + "/patches/" + patch.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "label", "labels", addLabelList);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("labelChangeError"));
            if (!resp.isSuccess()) {
                logger.warn("error adding {} labels to patch:{} resp:{}", addLabelList, patch, resp);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error adding label to patch: {}", patch, e);
        }
        return null;
    }

    public String createPatch(String title, String description, List<String> labels, String baseOid, String patchOid, GitRepository repo, String projectId) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPost(getHttpNodeUrl() + "/api/v1/projects/" + projectId + "/patches");
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("title", title, "description", description, "oid", patchOid, "target", baseOid, "labels", labels);
            var json = MAPPER.writeValueAsString(patchIssueData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(patchReq, RadicleBundle.message("createPatchError"));
            if (!resp.isSuccess()) {
                logger.warn("error creating new patch title:{} description:{} base_oid:{} patch_oid:{} repo:{} projectId:{}",
                        title, description, baseOid, patchOid, repo, projectId);
                return null;
            }
            var map = (Map<String, String>) MAPPER.readValue(resp.body, new TypeReference<>() { });
            return map.get("id");
        } catch (Exception e) {
            logger.warn("exception creating new patch title:{} description:{} base_oid:{} patch_oid:{} repo:{} projectId:{}",
                    title, description, baseOid, patchOid, repo, projectId, e);
        }
        return null;
    }

    public RadIssue fetchIssue(String projectId, GitRepository repo, String issueId) {
        var node = getSeedNode();
        var radProject = fetchRadProject(projectId);
        if (radProject == null) {
            return null;
        }
        var url = node.url + "/api/v1/projects/" + projectId + "/issues/" + issueId;
        try {
            var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchIssueError"));
            if (res.isSuccess()) {
                var issue = MAPPER.readValue(res.body, RadIssue.class);
                issue.repo = repo;
                issue.projectId = projectId;
                issue.project = repo.getProject();
                issue.seedNode = node;
                return issue;
            }
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
        }
        return null;
    }

    public RadIssue changeIssueState(RadIssue issue, String state) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "lifecycle", "state", Map.of("status", state, "reason", "other"));
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("stateChangeError"));
            if (!resp.isSuccess()) {
                logger.warn("error changing state {} to issue:{} resp:{}", state, issue, resp);
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error changing state to issue: {}", issue, e);
        }
        return null;
    }

    public RadIssue addRemoveIssueAssignees(RadIssue issue, List<String> addAssigneesList) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "assign", "assignees", addAssigneesList);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("assignersChangeError"));
            if (!resp.isSuccess()) {
                logger.warn("error adding {} assignees to issue:{} resp:{}", addAssigneesList, issue, resp);
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error adding / remove assignees to issue: {}", issue, e);
        }
        return null;
    }

    public RadPatch deleteRevisionComment(RadPatch patch, String revId, String commentId) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.radProject.id + "/patches/" + patch.id);
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            HashMap<String, Object> patchData = new HashMap<>(Map.of("type", "revision.comment.redact", "revision", revId, "comment", commentId));
            var json = MAPPER.writeValueAsString(patchData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(patchReq, RadicleBundle.message("revisionDeleteError"));
            if (!resp.isSuccess()) {
                logger.warn("error deleting revision comment {} , resp:{}", revId, resp);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error deleting revision comment {}", revId);
        }
        return null;
    }

    public RadPatch patchCommentReact(RadPatch patch, String commendId, String revId, String reaction, boolean active) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.radProject.id + "/patches/" + patch.id);
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchData = Map.of("type", "revision.comment.react", "revision", revId, "comment", commendId, "reaction", reaction, "active", active);
            var json = MAPPER.writeValueAsString(patchData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(patchReq, RadicleBundle.message("reactionError"));
            if (!resp.isSuccess()) {
                logger.warn("error reacting to revision:{} comment:{} resp:{}", revId, commendId, resp);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error reacting to revision:{} comment:{}", revId, commendId, e);
        }

        return null;
    }

    public RadIssue issueCommentReact(RadIssue issue, String discussionId, String reaction, boolean active) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var issueData = Map.of("type", "comment.react", "id", discussionId, "reaction", reaction, "active", active);
            var json = MAPPER.writeValueAsString(issueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("reactionError"));
            if (!resp.isSuccess()) {
                logger.warn("error reacting to discussion:{} resp:{}", discussionId, resp);
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error reacting to discussion: {}", discussionId, e);
        }

        return null;
    }

    public RadIssue addRemoveIssueLabel(RadIssue issue, List<String> addTagList) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "label", "labels", addTagList);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("labelChangeError"));
            if (!resp.isSuccess()) {
                logger.warn("error adding {} tags to issue:{} resp:{}", addTagList, issue, resp);
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error adding label to issue: {}", issue, e);
        }
        return null;
    }

    public RadIssue addIssueComment(RadIssue issue, String comment, String replyTo, List<Embed> embedList) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "comment", "body", comment, "replyTo", replyTo, "embeds", embedList);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("commentError"), RadicleBundle.message("commentDescError"));
            if (!resp.isSuccess()) {
                logger.warn("error adding comment: {} to patch:{} resp:{}", comment, issue, resp);
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error creating issue comment: {}", issue, e);
        }

        return null;
    }

    public RadIssue addIssueComment(RadIssue issue, String comment, List<Embed> embedList) {
        return addIssueComment(issue, comment, issue.id, embedList);
    }

    public RadIssue changeIssueTitle(RadIssue issue) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchEditData = Map.of("type", "edit", "title", issue.title);
            var json = MAPPER.writeValueAsString(patchEditData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("issueTitleError"));
            if (!resp.isSuccess()) {
                logger.warn("received invalid response with status:{} and body:{} while editing patch: {}",
                        resp.status, resp.body, issue);
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error changing issue title: {}", issue, e);
        }
        return null;
    }

    public RadPatch changePatchComment(String revisionId, String commentId, String body, RadPatch patch, List<Embed> embedList) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.radProject.id + "/patches/" + patch.id);
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchEditData = Map.of("type", "revision.comment.edit", "revision", revisionId, "comment", commentId, "body", body, "embeds", embedList);
            var json = MAPPER.writeValueAsString(patchEditData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(patchReq, RadicleBundle.message("commentEditError"), RadicleBundle.message("commentDescError"));
            if (!resp.isSuccess()) {
                logger.warn("received invalid response with status:{} and body:{} while editing patch: {}",
                        resp.status, resp.body, patch);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error changing patch comment: {}", patch, e);
        }

        return null;
    }

    public RadPatch changePatchTitle(RadPatch patch) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.radProject.id + "/patches/" + patch.id);
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchEditData = Map.of("type", "edit", "target", "delegates", "title",
                    Strings.nullToEmpty(patch.title));
            var json = MAPPER.writeValueAsString(patchEditData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(patchReq, RadicleBundle.message("patchTitleError"));
            if (!resp.isSuccess()) {
                logger.warn("received invalid response with status:{} and body:{} while editing patch: {}",
                        resp.status, resp.body, patch);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error changing patch title: {}", patch, e);
        }

        return null;
    }

    public RadPatch addPatchComment(RadPatch patch, String comment, RadDiscussion.Location location, List<Embed> embedList) {
        return addPatchComment(patch, comment, null, location, embedList);
    }

    public RadPatch addPatchComment(RadPatch patch, String comment, String replyTo, RadDiscussion.Location location, List<Embed> embedList) {
        var session = createAuthenticatedSession();
        if (session == null) {
            return null;
        }
        try {
            var commentReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.radProject.id + "/patches/" + patch.id);
            commentReq.setHeader("Authorization", "Bearer " + session.sessionId);
            HashMap<String, Object> data;
            boolean isReviewComment = false;
            if (location == null) {
                 data = new HashMap<>(Map.of("type", "revision.comment", "revision", patch.revisions.get(patch.revisions.size() - 1).id(),
                         "body", comment, "embeds", embedList));
            } else {
                 data = new HashMap<>(Map.of("type", "revision.comment", "revision", patch.revisions.get(patch.revisions.size() - 1).id(),
                        "body", comment, "embeds", embedList, "location", location.getMapObject()));
                isReviewComment = true;
            }
            if (!Strings.isNullOrEmpty(replyTo)) {
                data.put("replyTo", replyTo);
            }
            var json = MAPPER.writeValueAsString(data);
            commentReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var errorDescMsg = !isReviewComment ? RadicleBundle.message("commentDescError") : "";
            var resp = makeRequest(commentReq, RadicleBundle.message("commentError"), errorDescMsg);
            if (!resp.isSuccess()) {
                logger.warn("error adding comment: {} to patch:{} resp:{}", comment, patch, resp);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error adding comment to patch: {} - {}", patch, comment, e);
        }
        return null;
    }

    public RadAuthor resolveAlias(String nid) {
        if (Strings.isNullOrEmpty(nid)) {
            return null;
        }
        var alias = aliases.get(nid);
        if (alias != null) {
            return alias;
        }

        final var node = getSeedNode();
        final var url = node.url + "/api/v1/nodes/" + nid;
        try {
            var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchSeedNodeError"));
            if (res.isSuccess()) {
                alias = MAPPER.readValue(res.body, RadAuthor.class);
                alias.id = nid;
                aliases.put(nid, alias);
                return alias;
            }
        } catch (Exception e) {
            logger.warn("http request exception for resolving nid to alias: {}", url, e);
        }

        return null;
    }

    private Session createSession() {
        var url = getHttpNodeUrl() + "/api/v1/sessions";
        try {
            var post = new HttpPost(url);
            var resp = makeRequest(post, RadicleBundle.message("createSessionError"));
            if (!resp.isSuccess()) {
                return null;
            }
            var session = MAPPER.readValue(resp.body, Session.class);
            var authService = project.getService(AuthService.class);
            var signSession = authService.authenticate(session);
            return session.withSig(signSession);
        } catch (Exception e) {
            logger.warn("Unable to create session");
            return null;
        }
    }

    public Session createAuthenticatedSession() {
        var session = getCurrentSession();
        if (session != null) {
            return session;
        }
        var s = createSession();
        if (s == null || Strings.isNullOrEmpty(s.signature)) {
            ApplicationManager.getApplication().invokeLater(() ->
                    showNotification(project, RadicleBundle.message("authenticationError"), "",
                            NotificationType.ERROR, List.of()));
            return null;
        }

        var url = getHttpNodeUrl() + "/api/v1/sessions/" + s.sessionId;
        try {
            var data = Map.of("sessionId", s.sessionId, "pk", s.publicKey, "sig", s.signature);
            var jsonEntity = MAPPER.writeValueAsString(data);
            var put = new HttpPut(url);
            put.setEntity(new StringEntity(jsonEntity, ContentType.APPLICATION_JSON));
            var res = makeRequest(put, RadicleBundle.message("sessionError"));
            if (res.isSuccess()) {
                logger.info("created authenticated session: {}", s.sessionId);
                addSession(s);
                return s;
            } else {
                logger.warn("invalid response for authenticating session: {} - {}", s, res.status);
                return null;
            }
        } catch (Exception e) {
            logger.warn("error creating authenticated session: {}", s, e);
            return null;
        }
    }

    protected HttpResponseStatusBody makeRequest(HttpRequestBase req, String errorMsg) {
        return this.makeRequest(req, errorMsg, "");
    }

    protected HttpResponseStatusBody makeRequest(HttpRequestBase req, String errorMsg, String errorDesc) {
        CloseableHttpResponse resp = null;
        HttpResponseStatusBody responseStatusBody = null;
        try {
            resp = client.execute(req);
            String body = EntityUtils.toString(resp.getEntity());
            int status = resp.getStatusLine().getStatusCode();
            responseStatusBody = new HttpResponseStatusBody(status, body);
            return responseStatusBody;
        } catch (Exception e) {
            var errorMessage = !Strings.isNullOrEmpty(errorDesc) ? errorDesc : e.getMessage();
            ApplicationManager.getApplication().invokeLater(() ->
                    showNotification(project, RadicleBundle.message("httpRequestErrorTitle"), Strings.nullToEmpty(errorMessage), NotificationType.ERROR, null));
            logger.warn("error executing request", e);
            return new HttpResponseStatusBody(-1, "");
        } finally {
            if (resp != null) {
                try {
                    resp.close();
                    if (responseStatusBody != null && !responseStatusBody.isSuccess() && !Strings.isNullOrEmpty(errorMsg)) {
                        ApplicationManager.getApplication().invokeLater(() -> showNotification(project, errorMsg, errorDesc, NotificationType.ERROR, null));
                    }
                } catch (Exception e) {
                    logger.warn("error closing response", e);
                }
            }
        }
    }

    public void resetCurrentIdentity() {
        this.identity = null;
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

    protected Session getCurrentSession() {
        return getCurrentSession(getCurrentIdentity());
    }

    protected Session getCurrentSession(RadDetails rd) {
        if (rd == null || sessions.asMap().isEmpty()) {
            return null;
        }
        return sessions.getIfPresent(rd.nodeId);
    }

    protected void addSession(Session session) {
        sessions.put(session.publicKey, session);
    }

    protected SeedNode getSeedNode() {
        var sh = new RadicleProjectSettingsHandler(project);
        var settings = sh.loadSettings();
        return settings.getSeedNode();
    }

    protected String getHttpNodeUrl() {
        return getSeedNode().url;
    }

    public CloseableHttpClient getClient() {
        return client;
    }

    public Project getProject() {
        return project;
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

    public record SeedNodeInfo(String id, String version, String errorMessage) { }

    public record Session(String sessionId, String publicKey, String signature) {
        public Session withSig(String sig) {
            return new Session(sessionId(), publicKey(), sig);
        }
    }

    public record HttpResponseStatusBody(int status, String body) {
        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }

}
