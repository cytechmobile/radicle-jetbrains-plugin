package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.util.List;
import java.util.Map;

import static network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction.showNotification;

public class RadicleProjectApi {
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    private static final Logger logger = LoggerFactory.getLogger(RadicleProjectApi.class);
    private static final int PER_PAGE = 10;

    private final CloseableHttpClient client;
    private final Project project;

    protected Cache<String, Session> sessions;
    protected SeedNode seedNode;

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
            var res = makeRequest(new HttpGet(url));
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
        var url = node.url + "/api/v1/projects/" + projectId + "/issues";
        try {
            var res = makeRequest(new HttpGet(url));
            if (res.isSuccess()) {
                var issues = MAPPER.readValue(res.body, new TypeReference<List<RadIssue>>() { });
                for (var issue : issues) {
                    issue.repo = repo;
                    issue.projectId = projectId;
                    issue.project = repo.getProject();
                    issue.seedNode = node;
                }
                return issues;
            }
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
        }
        return List.of();
    }

    public List<RadPatch> fetchPatches(String projectId, GitRepository repo) {
        var radProject = fetchRadProject(projectId);
        if (radProject == null) {
            return List.of();
        }
        var node = getSeedNode();
        var url = node.url + "/api/v1/projects/" + projectId + "/patches";
        try {
            var res = makeRequest(new HttpGet(url));
            if (res.isSuccess()) {
                var patches = MAPPER.readValue(res.body, new TypeReference<List<RadPatch>>() { });
                for (var patch : patches) {
                    patch.seedNode = node;
                    patch.project = repo.getProject();
                    patch.projectId = projectId;
                    patch.defaultBranch = radProject.defaultBranch;
                    patch.repo = repo;
                }
                return patches;
            }
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
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
            var res = makeRequest(new HttpGet(url));
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
            var res = makeRequest(new HttpGet(url));
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
            var res = makeRequest(new HttpGet(url));
            if (res.isSuccess()) {
                return MAPPER.readValue(res.body, RadProject.class);
            }
            return null;
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return null;
        }
    }

    public RadIssue fetchIssue(String projectId, GitRepository repo, String issueId) {
        var node = getSeedNode();
        var radProject = fetchRadProject(projectId);
        if (radProject == null) {
            return null;
        }
        var url = node.url + "/api/v1/projects/" + projectId + "/issues/" + issueId;
        try {
            var res = client.execute(new HttpGet(url));
            String response = "";
            if (res != null) {
                response = EntityUtils.toString(res.getEntity());
            }
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                var issue = MAPPER.readValue(response, RadIssue.class);
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

    public RadIssue addIssueComment(RadIssue issue, String body) {
        //TODO check for expiration
        var session = createAuthenticatedSession(issue.repo);
        if (session == null) {
            return null;
        }
        try {
            var issueReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + issue.projectId + "/issues/" + issue.id);
            issueReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchEditData = Map.of("type", "thread", "action",
                    Map.of("type", "comment", "body", body));
            var json = MAPPER.writeValueAsString(patchEditData);
            issueReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = client.execute(issueReq);
            if (resp == null || resp.getStatusLine().getStatusCode() < 200 || resp.getStatusLine().getStatusCode() >= 300) {
                logger.warn("received invalid response with status:{} and body:{} while editing issue: {}",
                        resp == null ? "null resp" : resp.getStatusLine().getStatusCode(),
                        resp == null ? "null" : EntityUtils.toString(resp.getEntity()), issueReq);
                ApplicationManager.getApplication().invokeLater(() ->
                        showNotification(issue.project, RadicleBundle.message("commentError"), "",
                                NotificationType.ERROR, List.of()));
            } else {
                EntityUtils.consume(resp.getEntity());
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error creating issue comment: {}", issue, e);
            ApplicationManager.getApplication().invokeLater(() ->
                    showNotification(issue.project, RadicleBundle.message("commentError"),  "",
                            NotificationType.ERROR, List.of()));
        }

        return null;
    }

    public RadIssue changeIssueTitle(RadIssue issue) {
        //TODO check for expiration
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
            var resp = client.execute(issueReq);
            if (resp == null || resp.getStatusLine().getStatusCode() < 200 || resp.getStatusLine().getStatusCode() >= 300) {
                logger.warn("received invalid response with status:{} and body:{} while editing issue: {}",
                        resp == null ? "null resp" : resp.getStatusLine().getStatusCode(),
                        resp == null ? "null" : EntityUtils.toString(resp.getEntity()), issueReq);
                ApplicationManager.getApplication().invokeLater(() ->
                        showNotification(issue.project, RadicleBundle.message("issueTitleError"), "",
                                NotificationType.ERROR, List.of()));
            } else {
                EntityUtils.consume(resp.getEntity());
            }
            return issue;
        } catch (Exception e) {
            logger.warn("error changing issue title: {}", issue, e);
            ApplicationManager.getApplication().invokeLater(() ->
                    showNotification(issue.project, RadicleBundle.message("issueTitleError"), "",
                            NotificationType.ERROR, List.of()));
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
                    patch.title, "description", patch.description);
            var json = MAPPER.writeValueAsString(patchEditData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(patchReq);
            if (!resp.isSuccess()) {
                logger.warn("received invalid response with status:{} and body:{} while editing patch: {}",
                        resp.status, resp.body, patch);
                return null;
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error changing patch title: {}", patch, e);
            ApplicationManager.getApplication().invokeLater(() ->
                    showNotification(patch.project, RadicleBundle.message("patchTitleError"), "",
                            NotificationType.ERROR, List.of()));
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
            //"replyTo", ""
            var data = Map.of("type", "thread", "revision",
                    patch.revisions.get(patch.revisions.size() - 1).id(), "action", Map.of("type", "comment", "body", comment));
            var json = MAPPER.writeValueAsString(data);
            commentReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = makeRequest(commentReq);
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
            var res = client.execute(put);
            var status = res.getStatusLine().getStatusCode();
            EntityUtils.consume(res.getEntity());
            if (status >= 200 && status < 300) {
                logger.info("created authenticated session: {}", s.sessionId);
                addSession(s);
                return s;
            } else {
                logger.warn("invalid response for authenticating session: {} - {}", s, status);
                return null;
            }
        } catch (Exception e) {
            logger.warn("error creating authenticated session: {}", s, e);
            return null;
        }
    }

    protected HttpResponseStatusBody makeRequest(HttpRequestBase req) {
        CloseableHttpResponse resp = null;
        try {
            resp = client.execute(req);
            String body = EntityUtils.toString(resp.getEntity());
            int status = resp.getStatusLine().getStatusCode();
            return new HttpResponseStatusBody(status, body);
        } catch (Exception e) {
            logger.warn("error executing request", e);
            return new HttpResponseStatusBody(-1, "");
        } finally {
            if (resp != null) {
                try {
                    resp.close();
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
        if (seedNode != null) {
            return seedNode;
        }
        var sh = new RadicleProjectSettingsHandler(project);
        var settings = sh.loadSettings();
        seedNode = settings.getSeedNode();
        return seedNode;
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
