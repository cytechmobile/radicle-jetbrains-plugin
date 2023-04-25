package network.radicle.jetbrains.radiclejetbrainsplugin.providers;

import com.fasterxml.jackson.core.type.TypeReference;
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

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectApi {
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
        var url = node.url + "/api/v1/projects/" + projectId + "/patches";
        try {
            var res = client.execute(new HttpGet(url));
            String response = "";
            if (res != null) {
                response = EntityUtils.toString(res.getEntity());
            }
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                var objectMapper = new ObjectMapper();
                /* Support Instant type */
                objectMapper.registerModule(new JavaTimeModule());
                var patches = objectMapper.readValue(response, new TypeReference<List<RadPatch>>() { });
                for (var patch : patches) {
                    patch.repo = repo;
                }
                return patches;
            }
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
        }
        return List.of();
    }

    public List<RadProject> fetchRadProjects(SeedNode selectedNode, int page) {
        var url = selectedNode.url + "/api/v1/projects?per-page=" + PER_PAGE + "&page=" + page;
        try {
            var res = client.execute(new HttpGet(url));
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                String response = EntityUtils.toString(res.getEntity());
                return convertJsonToObject(response);
            }
            return null;
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return null;
        }
    }

    private List<RadProject> convertJsonToObject(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<HashMap<String, Object>> radProjects = mapper.readValue(json, new TypeReference<>() {
            });
            return radProjects.stream().map(n -> {
                var id = ((String) n.get("id"));
                var name = (String) n.get("name");
                var description = (String) n.get("description");
                return new RadProject(id, name, description);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Deserialization of rad project failed : {}", json, e);
            return List.of();
        }
    }

    public record SeedNodeInfo(String id, String version, String errorMessage) { }
}
