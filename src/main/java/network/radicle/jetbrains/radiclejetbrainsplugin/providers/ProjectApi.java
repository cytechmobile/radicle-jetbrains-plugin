package network.radicle.jetbrains.radiclejetbrainsplugin.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectApi {
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    private static final Logger logger = LoggerFactory.getLogger(ProjectApi.class);
    private static final int PER_PAGE = 10;
    private final HttpClient client;

    public ProjectApi() {
        this.client = HttpClientBuilder.create().build();
    }

    public ProjectApi(HttpClient client) {
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

    public List<RadPatch> fetchPatches(SeedNode node, String projectId, GitRepository repo) {
        var radProject = fetchRadProject(node, projectId);
        if (radProject == null) {
            return List.of();
        }
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

    public RadPatch fetchPatch(SeedNode node, String projectId, GitRepository repo, String patchId) {
        var radProject = fetchRadProject(node, projectId);
        if (radProject == null) {
            return null;
        }
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

    public List<RadProject> fetchRadProjects(SeedNode selectedNode, int page) {
        var url = selectedNode.url + "/api/v1/projects?per-page=" + PER_PAGE + "&page=" + page;
        try {
            var res = client.execute(new HttpGet(url));
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                String response = EntityUtils.toString(res.getEntity());
                return MAPPER.readValue(response, new TypeReference<>() { });
            }
            return null;
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return null;
        }
    }

    public RadProject fetchRadProject(SeedNode selectedNode, String projectId) {
        var url = selectedNode.url + "/api/v1/projects/" + projectId;
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

    public record SeedNodeInfo(String id, String version, String errorMessage) { }
}
