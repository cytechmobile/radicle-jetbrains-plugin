package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jnr.ffi.LibraryLoader;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RadicleNativeService {
    private static final Logger logger = Logger.getInstance(RadicleNativeService.class);

    public static JavaRad javaRad;
    public static boolean loadError = false;
    public static Path libFile;
    public static Map<String, String> aliases;

    private final Project project;

    public RadicleNativeService(Project project) {
        this.project = project;
        aliases = new HashMap<>();
        load();
    }

    public String radHome() {
        if (javaRad == null) {
            return null;
        }
        return javaRad.radHome("jchrist");
    }

    public JRadResponse editIssueTitleDescription(String repoId, String issueId, String title, String description, List<Embed> embeds) {
        if (javaRad == null) {
            return new JRadResponse(false, "native service not loaded");
        }

        try {
            var json = RadicleCliService.MAPPER.writeValueAsString(
                    Map.of("repo_id", repoId,
                            "issue_id", issueId,
                            "title", Strings.nullToEmpty(title),
                            "description", Strings.nullToEmpty(description),
                            "embeds", embeds == null ? List.of() : embeds));
            var res = javaRad.changeIssueTitleDescription(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("error response from native service: " + resp);
                if (resp == null) {
                    resp = new JRadResponse(false, "no resp");
                }
            }
            return resp;
        } catch (Throwable e) {
            logger.warn("Error changing issue title", e);
            return new JRadResponse(false, e.getMessage());
        }
    }

    public Map<String, String> getEmbeds(String repoId, List<String> oids) {
        if (javaRad == null) {
            return null;
        }

        try {
            var json = RadicleCliService.MAPPER.writeValueAsString(Map.of("repo_id", repoId, "oids", oids));
            var res = javaRad.getEmbeds(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("error response from native service: " + resp);
                return null;
            }
            var tree = RadicleCliService.MAPPER.readTree(res);
            var jsonMap = tree.get("result");
            return RadicleCliService.MAPPER.convertValue(jsonMap, new TypeReference<>() { });
        } catch (Throwable e) {
            logger.warn("Error getting embeds", e);
            return null;
        }
    }

    public String getAlias(String nid) {
        nid = normalizeNid(nid);
        if (aliases.containsKey(nid)) {
            return aliases.get(nid);
        }
        if (javaRad == null) {
            return null;
        }
        var result = getAlias(List.of(nid));
        return result.get(nid);
    }

    public Map<String, String> getAlias(List<String> nids) {
        nids = nids.stream().map(RadicleNativeService::normalizeNid).toList();
        Map<String, String> result = new HashMap<>();
        Set<String> missing = new HashSet<>(nids);
        for (var nid : nids) {
            if (aliases.containsKey(nid)) {
                result.put(nid, aliases.get(nid));
                missing.remove(nid);
            }
        }
        if (missing.isEmpty()) {
            return result;
        }
        if (javaRad == null) {
            return null;
        }
        try {
            var json = RadicleCliService.MAPPER.writeValueAsString(Map.of("ids", missing));
            var res = javaRad.getAlias(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("error response from native service for aliases: " + resp);
                // add a value to signal the error and not retry it
                for (var nid : missing) {
                    aliases.put(nid, "");
                }
                return result;
            }
            var tree = RadicleCliService.MAPPER.readTree(res);
            var jsonMap = tree.get("result");
            var resolvedMap = RadicleCliService.MAPPER.convertValue(jsonMap, new TypeReference<Map<String, String>>() { });
            missing.removeAll(resolvedMap.keySet());
            aliases.putAll(resolvedMap);
            result.putAll(resolvedMap);
            if (!missing.isEmpty()) {
                for (var nid : missing) {
                    aliases.put(nid, "");
                }
            }
            return result;
        } catch (Throwable e) {
            logger.warn("Error getting aliases for nids:" + nids, e);
            for (var nid : missing) {
                aliases.put(nid, "");
            }
            return null;
        }
    }

    public boolean createPatchComment(
            String repoId, String patchId, String revisionId, String comment, String replyTo, RadDiscussion.Location location, List<Embed> embeds) {
        if (javaRad == null) {
            return false;
        }
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("repo_id", repoId);
            jsonMap.put("patch_id", patchId);
            jsonMap.put("revision_id", revisionId);
            jsonMap.put("comment", comment);
            jsonMap.put("reply_to", Strings.nullToEmpty(replyTo));
            jsonMap.put("location", location == null ? null : location.getMapObject());
            jsonMap.put("embeds", embeds == null ? List.of() : embeds);
            var json = RadicleCliService.MAPPER.writeValueAsString(jsonMap);
            var res = javaRad.createPatchComment(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("Error creating patch comment:" + resp);
                return false;
            }
            return true;
        } catch (Throwable e) {
            logger.warn("Error creating patch comment", e);
            return false;
        }
    }

    public boolean editPatchComment(
            String repoId, String patchId, String revisionId, String commentId, String comment, List<Embed> embeds) {
        if (javaRad == null) {
            return false;
        }
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("repo_id", repoId);
            jsonMap.put("patch_id", patchId);
            jsonMap.put("revision_id", revisionId);
            jsonMap.put("comment_id", commentId);
            jsonMap.put("comment", comment);
            jsonMap.put("embeds", embeds == null ? List.of() : embeds);
            var json = RadicleCliService.MAPPER.writeValueAsString(jsonMap);
            var res = javaRad.editPatchComment(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("Error creating patch comment:" + resp);
                return false;
            }
            return true;
        } catch (Throwable e) {
            logger.warn("Error creating patch comment", e);
            return false;
        }
    }

    public boolean deletePatchComment(String repoId, String patchId, String revisionId, String commentId) {
        if (javaRad == null) {
            return false;
        }
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("repo_id", repoId);
            jsonMap.put("patch_id", patchId);
            jsonMap.put("revision_id", revisionId);
            jsonMap.put("comment_id", commentId);
            jsonMap.put("comment", "");
            jsonMap.put("embeds", List.of());
            var json = RadicleCliService.MAPPER.writeValueAsString(jsonMap);
            var res = javaRad.deletePatchComment(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("Error deleting patch comment:" + resp);
                return false;
            }
            return true;
        } catch (Throwable e) {
            logger.warn("Error deleting patch comment", e);
            return false;
        }
    }

    public boolean editIssueComment(
            String repoId, String issueId, String commentId, String comment, List<Embed> embeds) {
        if (javaRad == null) {
            return false;
        }
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("repo_id", repoId);
            jsonMap.put("issue_id", issueId);
            jsonMap.put("comment_id", commentId);
            jsonMap.put("comment", comment);
            jsonMap.put("embeds", embeds == null ? List.of() : embeds);
            var json = RadicleCliService.MAPPER.writeValueAsString(jsonMap);
            var res = javaRad.editIssueComment(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("Error editing issue comment:" + resp);
                return false;
            }
            return true;
        } catch (Throwable e) {
            logger.warn("Error editing issue comment", e);
            return false;
        }
    }

    public boolean patchCommentReact(
            String repoId, String patchId, String revisionId, String commentId, String reaction, boolean active) {
        if (javaRad == null) {
            return false;
        }
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("repo_id", repoId);
            jsonMap.put("patch_id", patchId);
            jsonMap.put("revision_id", revisionId);
            jsonMap.put("comment_id", commentId);
            jsonMap.put("reaction", reaction);
            jsonMap.put("active", active);
            var json = RadicleCliService.MAPPER.writeValueAsString(jsonMap);
            var res = javaRad.patchCommentReact(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("Error adding patch comment reaction:" + resp);
                return false;
            }
            return true;
        } catch (Throwable e) {
            logger.warn("Error adding patch comment reaction", e);
            return false;
        }
    }

    public boolean issueCommentReact(
            String repoId, String issueId, String commentId, String reaction, boolean active) {
        if (javaRad == null) {
            return false;
        }
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("repo_id", repoId);
            jsonMap.put("issue_id", issueId);
            jsonMap.put("comment_id", commentId);
            jsonMap.put("reaction", reaction);
            jsonMap.put("active", active);
            var json = RadicleCliService.MAPPER.writeValueAsString(jsonMap);
            var res = javaRad.issueCommentReact(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("Error adding issue comment reaction:" + resp);
                return false;
            }
            return true;
        } catch (Throwable e) {
            logger.warn("Error adding issue comment reaction", e);
            return false;
        }
    }

    public static String normalizeNid(String nid) {
        nid = Strings.nullToEmpty(nid);
        if (nid.startsWith("did:key:")) {
            nid = nid.substring(8);
        }
        return nid;
    }

    public static Path createTempFileFromJar(final String library) throws Exception {
        // create temp file
        var tempDir = Files.createTempDirectory("radicle-jetbrains-plugin-native-" + System.currentTimeMillis());
        var tempFile = Files.createFile(tempDir.resolve(library));
        try (var is = RadicleNativeService.class.getResourceAsStream("/META-INF/jrad/" + library)) {
            Files.copy(Objects.requireNonNull(is), tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            logger.warn("error opening library resource stream: " + e);
        } finally {
            // set folder to be deleted after VM shuts down
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
        }
        return tempFile;
    }

    public static void load() {
        if (javaRad != null || loadError) {
            return;
        }
        try {
            var libName = System.mapLibraryName("jrad");
            libFile = createTempFileFromJar(libName);
            javaRad = LibraryLoader.create(JavaRad.class)
                    .search(Paths.get("./jrad/target/release").toAbsolutePath().normalize().toString())
                    .search(Paths.get("./jrad/target/debug").toAbsolutePath().normalize().toString())
                    .search(libFile == null ? "." : libFile.toAbsolutePath().getParent().toString())
                    .failImmediately()
                    .load("jrad");
        } catch (Throwable t) {
            logger.warn("Error loading native library", t);
            loadError = true;
        }
    }

    public record JRadResponse(boolean ok, String msg) { }

    public interface JavaRad {
        String radHome(String input);
        String changeIssueTitleDescription(String input);
        String getEmbeds(String input);
        String getAlias(String input);
        String createPatchComment(String input);
        String editPatchComment(String input);
        String deletePatchComment(String input);
        String editIssueComment(String input);
        String patchCommentReact(String input);
        String issueCommentReact(String input);
    }
}
