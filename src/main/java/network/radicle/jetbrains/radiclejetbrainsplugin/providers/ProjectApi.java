package network.radicle.jetbrains.radiclejetbrainsplugin.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public List<RadProject> fetchRadProjects(SeedNode selectedNode, int page) {
        var url = "https://" + selectedNode.host + ":" + selectedNode.port + "/v1/projects?per-page=" + PER_PAGE +
                "&page=" + page;
        try {
            var res = client.execute(new HttpGet(url));
            if (res != null && res.getStatusLine().getStatusCode() == 200) {
                String response = EntityUtils.toString(res.getEntity());
                return convertJsonToObject(response, selectedNode);
            }
            return null;
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return null;
        }
    }

    private List<RadProject> convertJsonToObject(String json, SeedNode selectedNode) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<HashMap<String, Object>> radProjects = mapper.readValue(json, new TypeReference<>() { });
            return radProjects.stream().map(n -> {
                var urn = ((String) n.get("urn"));
                var urnParts = urn.split(":");
                var projectId = urnParts.length > 2 ? urnParts[2] : "";
                var name = (String) n.get("name");
                var description = (String) n.get("description");
                var radUrl = "rad://" + selectedNode.host + "/" + projectId;
                return new RadProject(urn, name, description, radUrl);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Deserialization of rad project failed : {}", json, e);
            return List.of();
        }
    }

}
