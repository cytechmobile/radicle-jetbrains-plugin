package network.radicle.jetbrains.radiclejetbrainsplugin.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectApi {
    private static final Logger logger = LoggerFactory.getLogger(ProjectApi.class);
    private static final int TIMEOUT = 10;
    private static final int PER_PAGE = 10;

    public static List<RadProject> fetchRadProjects(SeedNode selectedNode, int page) {
        var url = "https://" + selectedNode.host + ":" + selectedNode.port + "/v1/projects?per-page=" + PER_PAGE +
                "&page=" + page;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .timeout(Duration.ofSeconds(TIMEOUT))
                    .build();
            var res = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (res != null && res.statusCode() == 200) {
                return convertJsonToObject(res.body(), selectedNode);
            }
            return null;
        } catch (Exception e) {
            logger.warn("http request exception {}", url, e);
            return null;
        }
    }

    private static List<RadProject> convertJsonToObject(String json, SeedNode selectedNode) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<HashMap<String, Object>> radProjects = mapper.readValue(json, new TypeReference<>() {});
            return radProjects.stream().map(n -> {
                var urnParts = ((String) n.get("urn")).split(":");
                var urn = urnParts.length > 2 ? urnParts[2] : "";
                var name = (String) n.get("name");
                var description = (String) n.get("description");
                var radUrl = "rad://" + selectedNode.host + "/" + urn;
                return new RadProject(urn, name, description, radUrl);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Deserialization of rad project failed : {}", json, e);
            return List.of();
        }
    }

}
