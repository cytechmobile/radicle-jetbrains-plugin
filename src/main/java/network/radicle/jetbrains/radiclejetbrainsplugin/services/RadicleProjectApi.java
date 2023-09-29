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
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadWeb;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction.showNotification;

public class RadicleProjectApi {
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    public static final int ALL_IN_ONE_PAGE = 10000;
    private static final Logger logger = LoggerFactory.getLogger(RadicleProjectApi.class);
    private static final int PER_PAGE = 10;

    private final CloseableHttpClient client;
    private final Project project;

    protected Cache<String, Session> sessions;

    public RadicleProjectApi(Project project) {
        this(project, HttpClientBuilder.create().build());
    }

    @NonInjectable
    public RadicleProjectApi(Project project, CloseableHttpClient client) {
        this.project = project;
        this.client = client;
        this.sessions = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();
    }

    public SeedNodeInfo checkApi(SeedNode node) {
        var url = node.url + "/api/v1";
        try {
            var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchSeedNodeError"));
            if (res.isSuccess()) {
                var json = new ObjectMapper().readTree(res.body);
                String version = json.get("version").asText("");
                String id = json.get("node").get("id").asText("");
                return new SeedNodeInfo(id, version, null);
            }
            return new SeedNodeInfo(null, null, "HTTP Status code: " + res.status + " " + res.body);
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return new SeedNodeInfo(null, null, "Exception: " + e.getMessage());
        }
    }

    public List<RadIssue> fetchIssues(String projectId, GitRepository repo) {
        var node = getSeedNode();
        var allIssues = new ArrayList<RadIssue>();
        var states = Arrays.stream(RadIssue.State.values()).map(e -> e.status).toList();
        try {
            for (var state : states) {
                var url = node.url + "/api/v1/projects/" + projectId + "/issues?page=0&state=" + state + "&perPage=" + ALL_IN_ONE_PAGE;
                var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchIssuesError"));
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
        var radProject = fetchRadProject(projectId);
        if (radProject == null) {
            return List.of();
        }
        var node = getSeedNode();
        var states = Arrays.stream(RadPatch.State.values()).map(e -> e.status).toList();
        var allPatches = new ArrayList<RadPatch>();
        try {
            for (var state : states) {
                var url = node.url + "/api/v1/projects/" + projectId + "/patches?page=0&state=" + state + "&perPage=" + ALL_IN_ONE_PAGE;
                var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchPatchesError"));
                if (res.isSuccess()) {
                    var patches = MAPPER.readValue(res.body, new TypeReference<List<RadPatch>>() { });
                    for (var patch : patches) {
                        patch.seedNode = node;
                        patch.project = repo.getProject();
                        patch.projectId = projectId;
                        patch.defaultBranch = radProject.defaultBranch;
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

    public RadPatch fetchPatch(String projectId, GitRepository repo, String patchId) {
        var radProject = fetchRadProject(projectId);
        if (radProject == null) {
            return null;
        }
        var node = getSeedNode();
        final var url = node.url + "/api/v1/projects/" + projectId + "/patches/" + patchId;
        try {
            var res = makeRequest(new HttpGet(url), RadicleBundle.message("fetchPatchError"));
            if (res.isSuccess()) {
                var patch = MAPPER.readValue(res.body, RadPatch.class);
                patch.seedNode = node;
                patch.project = repo.getProject();
                patch.projectId = projectId;
                patch.defaultBranch = radProject.defaultBranch;
                patch.repo = repo;
                return patch;
            }
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
        }
        return null;
    }

    public List<RadProject> fetchRadProjects(int page) {
        var url = getHttpNodeUrl() + "/api/v1/projects?per-page=" + PER_PAGE + "&page=" + page;
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
            return null;
        }
    }

    public boolean createIssue(String title, String description, List<String> assignees,
                               List<String> labels, GitRepository repo, String projectId) {
        var session = createAuthenticatedSession(repo);
        if (session == null) {
            return false;
        }
        try {
            var issueReq = new HttpPost(getHttpNodeUrl() + "/api/v1/projects/" + projectId + "/issues");
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("title", title, "description", description, "labels", labels, "assignees", assignees);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("createIssueError"));
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
        var session = createAuthenticatedSession(patch.repo);
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.projectId + "/patches/" + patch.id);
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
        var session = createAuthenticatedSession(patch.repo);
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.projectId + "/patches/" + patch.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "label", "labels", addLabelList);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("tagChangeError"));
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
        var session = createAuthenticatedSession(issue.repo);
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
        var session = createAuthenticatedSession(issue.repo);
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

    public RadPatch patchCommentReact(RadPatch patch, String commendId, String revId, String reaction, boolean active) {
        var session = createAuthenticatedSession(patch.repo);
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.projectId + "/patches/" + patch.id);
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchData = Map.of("type", "revision.comment.react", "revision", revId, "comment", commendId, "reaction", reaction, "active", active);
            var json = MAPPER.writeValueAsString(patchData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(patchReq, RadicleBundle.message("reactionError"));
            if (!resp.isSuccess()) {
                logger.warn("error reacting to revision : {} , comment : {} , resp:{}", revId, commendId, resp);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error reacting to revision : {} , comment : {}", revId, commendId, e);
        }

        return null;
    }

    public RadIssue issueCommentReact(RadIssue issue, String discussionId, String reaction, boolean active) {
        var session = createAuthenticatedSession(issue.repo);
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
                logger.warn("error reacting to discussion : {} , resp:{}", discussionId, resp);
                return null;
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error reacting to discussion: {}", discussionId, e);
        }

        return null;
    }

    public RadIssue addRemoveIssueLabel(RadIssue issue, List<String> addTagList) {
        var session = createAuthenticatedSession(issue.repo);
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

    public RadIssue addIssueComment(RadIssue issue, String comment) {
        var session = createAuthenticatedSession(issue.repo);
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchIssueData = Map.of("type", "comment", "body", comment, "replyTo", issue.id);
            var json = MAPPER.writeValueAsString(patchIssueData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(issueReq, RadicleBundle.message("commentError"));
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

    public RadIssue changeIssueTitle(RadIssue issue) {
        var session = createAuthenticatedSession(issue.repo);
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

    public RadPatch changePatchTitle(RadPatch patch) {
        var session = createAuthenticatedSession(patch.repo);
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.projectId + "/patches/" + patch.id);
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchEditData = Map.of("type", "edit", "target", "delegates", "title",
                    Strings.nullToEmpty(patch.title), "description", Strings.nullToEmpty(patch.description));
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

    public RadPatch addPatchComment(RadPatch patch, String comment) {
        var session = createAuthenticatedSession(patch.repo);
        if (session == null) {
            return null;
        }
        try {
            var commentReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.projectId + "/patches/" + patch.id);
            commentReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var data = Map.of("type", "revision.comment", "revision", patch.revisions.get(patch.revisions.size() - 1).id(), "body", comment);
            var json = MAPPER.writeValueAsString(data);
            commentReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(commentReq, RadicleBundle.message("commentError"));
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

    public Session createAuthenticatedSession(GitRepository repo) {
        var session = getCurrentSession();
        if (session != null) {
            return session;
        }

        var radWeb = new RadWeb(repo);
        var output = radWeb.perform();
        var json = output.getStdout();
        Session s;
        try {
            s = MAPPER.readValue(json, Session.class);
        } catch (Exception e) {
            logger.warn("error parsing session info from cli: {}", json, e);
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
        CloseableHttpResponse resp = null;
        HttpResponseStatusBody responseStatusBody = null;
        try {
            resp = client.execute(req);
            String body = EntityUtils.toString(resp.getEntity());
            int status = resp.getStatusLine().getStatusCode();
            responseStatusBody = new HttpResponseStatusBody(status, body);
            return responseStatusBody;
        } catch (Exception e) {
            logger.warn("error executing request", e);
            return new HttpResponseStatusBody(-1, "");
        } finally {
            if (resp != null) {
                try {
                    resp.close();
                    if (responseStatusBody != null && !responseStatusBody.isSuccess() &&
                            !Strings.isNullOrEmpty(errorMsg)) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                showNotification(project, errorMsg, "",
                                        NotificationType.ERROR, List.of()));
                    }
                } catch (Exception e) {
                    logger.warn("error closing response", e);
                }
            }
        }
    }

    public RadDetails getCurrentIdentity() {
        var radSelf = new RadSelf(project);
        radSelf.askForIdentity(false);
        return radSelf.getRadSelfDetails();
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

    public record SeedNodeInfo(String id, String version, String errorMessage) { }

    public record Session(String sessionId, String publicKey, String signature) { }

    public record HttpResponseStatusBody(int status, String body) {
        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }
}
