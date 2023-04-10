package network.radicle.jetbrains.radiclejetbrainsplugin.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.ArrayList;
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
                return convertJsonPatchToOject(response, repo);
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

    private List<RadPatch.Comment> getComments(ArrayList<HashMap<String, Object>> comments) {
        try {
            var commentList = new ArrayList<RadPatch.Comment>();
            for (var com : comments) {
                var id = (String) com.get("id");
                var author = ((HashMap<String, String>) com.get("author"));
                var authorId = author.get("id");
                var body = (String) com.get("body");
                var created = Instant.ofEpochSecond((Integer) com.get("timestamp"));
                var replayTo = (String) com.get("replyTo");
                commentList.add(new RadPatch.Comment(id, authorId, body, created, replayTo));
            }
            return commentList;
        } catch (Exception e) {
            logger.warn("Deserialization of rad comments failed : {}", comments, e);
        }
        return List.of();
    }

    private RadPatch.State getState(String state) {
        return switch (state) {
            case "open" -> RadPatch.State.OPEN;
            case "closed" -> RadPatch.State.CLOSED;
            case "merged" -> RadPatch.State.MERGED;
            default -> null;
        };
    }

    private List<RadPatch.Merge> getMerges(ArrayList<HashMap<String, Object>> merges) {
        try {
            var mergeList = new ArrayList<RadPatch.Merge>();
            for (var merge : merges) {
                var node = (String) merge.get("node");
                var commit = (String) merge.get("commit");
                var timestamp = Instant.ofEpochSecond((Integer) merge.get("timestamp"));
                mergeList.add(new RadPatch.Merge(node, commit, timestamp));
            }
            return mergeList;
        } catch (Exception e) {
            logger.warn("Deserialization of rad merges failed : {}", merges, e);
            return List.of();
        }
    }

    private List<RadPatch.Revision> getRevisions(ArrayList<HashMap<String, Object>> revisions) {
        try {
            var revisionList = new ArrayList<RadPatch.Revision>();
            for (var rev : revisions) {
                var pId = (String) rev.get("id");
                var desc = (String) rev.get("description");
                var base = (String) rev.get("base");
                var commitHash = (String) rev.get("oid");
                var created = Instant.ofEpochSecond((Integer) rev.get("timestamp"));
                var refs = (ArrayList<String>) rev.get("refs");
                var discussions = ((ArrayList<HashMap<String, Object>>) rev.get("discussions"));
                var merges = ((ArrayList<HashMap<String, Object>>) rev.get("merges"));
                revisionList.add(new RadPatch.Revision(pId, desc, base, commitHash, created, refs, getComments(discussions),
                        getMerges(merges)));
            }
            return revisionList;
        } catch (Exception e) {
            logger.warn("Deserialization of rad revisions failed : {}", revisions, e);
            return List.of();
        }
    }

    private List<RadPatch> convertJsonPatchToOject(String json, GitRepository repo) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<HashMap<String, Object>> radPatches = mapper.readValue(json, new TypeReference<>() {
            });
            return radPatches.stream().map(n -> {
                var patchId = ((String) n.get("id"));
                var author = ((HashMap<String, String>) n.get("author"));
                var authorId = author.get("id");
                var title = ((String) n.get("title"));
                var description = ((String) n.get("description"));
                var target = ((String) n.get("target"));
                var revisions = ((ArrayList<HashMap<String, Object>>) n.get("revisions"));
                var stateMap = ((HashMap<String, String>) n.get("state"));
                var state = stateMap.get("status");
                return new RadPatch(repo, patchId, authorId, title, description, getState(state), target,
                        getRevisions(revisions));
            }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Deserialization of rad patches failed : {}", json, e);
            return List.of();
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
