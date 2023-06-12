package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.serviceContainer.NonInjectable;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadWeb;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RadicleProjectApi {
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    private static final Logger logger = LoggerFactory.getLogger(RadicleProjectApi.class);
    private static final int PER_PAGE = 10;
    private static final Key<Map<String, Session>> RADICLE_SESSIONS_KEY = new Key<>("RadicleHttpSessions");
    private final HttpClient client;
    private final Project project;

    protected Map<String, Session> sessions;
    protected SeedNode seedNode;

    public RadicleProjectApi(Project project) {
        this(project, HttpClientBuilder.create().build());
    }

    @NonInjectable
    public RadicleProjectApi(Project project, HttpClient client) {
        this.project = project;
        this.client = client;
    }

    public SeedNodeInfo checkApi(SeedNode node) {
        var url = node.url + "/api/v1";
        try {
            var res = client.execute(new HttpGet(url));
            String response = "";
            if (res != null) {
                response = EntityUtils.toString(res.getEntity());
            }

            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                var json = new ObjectMapper().readTree(response);
                String version = json.get("version").asText("");
                String id = json.get("node").get("id").asText("");
                return new SeedNodeInfo(id, version, null);
            }
            var statusCode = res != null  ? res.getStatusLine().getStatusCode() : "";
            return new SeedNodeInfo(null, null, "HTTP Status code: " + statusCode + " " + response);
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return new SeedNodeInfo(null, null, "Exception: " + e.getMessage());
        }
    }

    public List<RadPatch> fetchPatches(String projectId, GitRepository repo) {
        var radProject = fetchRadProject(projectId);
        if (radProject == null) {
            return List.of();
        }
        var node = getSeedNode();
        var url = node.url + "/api/v1/projects/" + projectId + "/patches";
        try {
            var res = client.execute(new HttpGet(url));
            String response = "";
            if (res != null) {
                response = EntityUtils.toString(res.getEntity());
            }
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                var patches = MAPPER.readValue(response, new TypeReference<List<RadPatch>>() { });
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
            var res = client.execute(new HttpGet(url));
            String response = "";
            if (res != null) {
                response = EntityUtils.toString(res.getEntity());
            }
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                var patch = MAPPER.readValue(response, RadPatch.class);
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
            var res = client.execute(new HttpGet(url));
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                String response = EntityUtils.toString(res.getEntity());
                if (Strings.isNullOrEmpty(response)) {
                    return new ArrayList<>();
                }
                return MAPPER.readValue(response, new TypeReference<>() { });
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
            var res = client.execute(new HttpGet(url));
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                String response = EntityUtils.toString(res.getEntity());
                return MAPPER.readValue(response, RadProject.class);
            }
            return null;
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return null;
        }
    }

    public RadPatch changePatchTitle(RadPatch patch) {
        var session = createAuthenticatedSession(patch.repo);
        if (session == null) {
            return null;
        }
        try {
            var patchReq = new HttpPatch(getHttpNodeUrl() + "/api/v1/projects/" + patch.projectId + "/patches/" + patch.id);
            patchReq.setHeader("Authorization", "Bearer " + session.sessionId);
            var patchEditData = Map.of("type", "edit", "target", "delegates", "title", patch.title);
            var json = MAPPER.writeValueAsString(patchEditData);
            patchReq.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            var resp = client.execute(patchReq);
            if (resp == null || resp.getStatusLine().getStatusCode() < 200 || resp.getStatusLine().getStatusCode() >= 300) {
                logger.warn("received invalid response with status:{} and body:{} while editing patch: {}",
                        resp == null ? "null resp" : resp.getStatusLine().getStatusCode(),
                        resp == null ? "null" : EntityUtils.toString(resp.getEntity()), patch);
            }
            return patch;
        } catch (Exception e) {
            logger.warn("error changing patch title: {}", patch, e);
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

    protected RadDetails getCurrentIdentity() {
        var radSelf = new RadSelf(project);
        return radSelf.getRadSelfDetails();
    }

    protected Session getCurrentSession() {
        return getCurrentSession(getCurrentIdentity());
    }

    protected Session getCurrentSession(RadDetails rd) {
        if (rd == null) {
            return null;
        }
        return sessions.get(rd.nodeId);
    }

    protected Map<String, Session> getSessions() {
        return sessions;
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

    public HttpClient getClient() {
        return client;
    }

    public Project getProject() {
        return project;
    }

    public record SeedNodeInfo(String id, String version, String errorMessage) { }

    public record Session(String sessionId, String publicKey, String signature) { }
}
