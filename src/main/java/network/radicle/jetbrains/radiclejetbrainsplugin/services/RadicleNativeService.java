package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import com.intellij.execution.util.ExecUtil;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import jnr.ffi.LibraryLoader;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RadicleNativeService {
    private static final Logger logger = Logger.getInstance(RadicleNativeService.class);

    public JRad jRad;
    public boolean loadError = false;
    public Path libFile;
    public Map<String, String> aliases;

    private final Project project;

    public RadicleNativeService(Project project) {
        this.project = project;
        aliases = new HashMap<>();
    }

    public String radHome() {
        var jrad = checkGetJRad();
        if (jrad == null) {
            return null;
        }
        return jrad.radHome("");
    }

    public JRadResponse editIssueTitleDescription(String repoId, String issueId, String title, String description, List<Embed> embeds) {
        var jrad = checkGetJRad();
        if (jrad == null) {
            return new JRadResponse(false, "native service not loaded");
        }

        try {
            var json = RadicleCliService.MAPPER.writeValueAsString(
                    Map.of("repo_id", repoId,
                            "issue_id", issueId,
                            "title", Strings.nullToEmpty(title),
                            "description", Strings.nullToEmpty(description),
                            "embeds", embeds == null ? List.of() : embeds));
            var res = jrad.changeIssueTitleDescription(json);
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
        var jrad = checkGetJRad();
        if (jrad == null) {
            return null;
        }

        try {
            var json = RadicleCliService.MAPPER.writeValueAsString(Map.of("repo_id", repoId, "oids", oids));
            var res = jrad.getEmbeds(json);
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
        var jrad = checkGetJRad();
        if (jrad == null) {
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
        var jrad = checkGetJRad();
        if (jrad == null) {
            return null;
        }
        try {
            var json = RadicleCliService.MAPPER.writeValueAsString(Map.of("ids", missing));
            var res = jrad.getAlias(json);
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
        var jrad = checkGetJRad();
        if (jrad == null) {
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
            var res = jrad.createPatchComment(json);
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
        var jrad = checkGetJRad();
        if (jrad == null) {
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
            var res = jrad.editPatchComment(json);
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
        var jrad = checkGetJRad();
        if (jrad == null) {
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
            var res = jrad.deletePatchComment(json);
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
        var jrad = checkGetJRad();
        if (jrad == null) {
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
            var res = jrad.editIssueComment(json);
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
        var jrad = checkGetJRad();
        if (jrad == null) {
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
            var res = jrad.patchCommentReact(json);
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
        var jrad = checkGetJRad();
        if (jrad == null) {
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
            var res = jrad.issueCommentReact(json);
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

    public JRad checkGetJRad() {
        load();
        return jRad;
    }

    public void load() {
        if (jRad != null || loadError) {
            return;
        }
        if (SystemInfo.isWindows) {
            // load the native binary to WSL instead
            loadWindowsJavaRad();
            return;
        }
        try {
            var libName = System.mapLibraryName("jrad");
            libFile = createTempFileFromJar(libName);
            jRad = LibraryLoader.create(JRad.class)
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

    public void loadWindowsJavaRad() {
        try {
            var rad = project.getService(RadicleProjectService.class);
            var wsl = new WSLDistribution(rad.getWslDistro());
            final var wslDirPath = "/tmp/radicle-jetbrains-plugin-native-" + System.currentTimeMillis();
            var tempDir = wsl.getWindowsPath(wslDirPath);
            var tempDirPath = Paths.get(tempDir);
            Files.createDirectories(tempDirPath);
            var tempFile = Files.createFile(tempDirPath.resolve("jrad"));
            final var arch = SystemInfo.isAarch64 ? "aarch64" : "x86_64"; // either aarch64 or x86_64
            try (var is = RadicleNativeService.class.getResourceAsStream("/META-INF/jrad/" + arch + "/jrad")) {
                Files.copy(Objects.requireNonNull(is), tempFile, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                // set folder to be deleted after VM shuts down
                tempFile.toFile().deleteOnExit();
                tempDirPath.toFile().deleteOnExit();
            }
            try {
                Files.setPosixFilePermissions(tempFile, Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));
            } catch (Exception e) {
                logger.debug("Unable to change wsl binary file permissions", e);
            }
            final var wslBinPath = wslDirPath + "/jrad";
            jRad = new WindowsJRad(project, wslBinPath);
        } catch (Throwable e) {
            logger.warn("error creating WSL temp directory", e);
            loadError = true;
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
        final var arch = SystemInfo.isAarch64 ? "aarch64" : "x86_64"; // either aarch64 or x86_64
        try (var is = RadicleNativeService.class.getResourceAsStream("/META-INF/jrad/" + arch + "/" + library)) {
            Files.copy(Objects.requireNonNull(is), tempFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            // set folder to be deleted after VM shuts down
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
        }
        return tempFile;
    }

    public record JRadResponse(boolean ok, String msg) { }

    public interface JRad {
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

    public static class WindowsJRad implements JRad {
        public final Project project;
        public final String wslBin;
        public final RadicleProjectService rad;

        public WindowsJRad(Project project, String binFilePath) {
            this.project = project;
            this.wslBin = binFilePath;
            this.rad = project.getService(RadicleProjectService.class);
        }

        @Override
        public String radHome(String input) {
            return execute("radHome", input);
        }

        @Override
        public String changeIssueTitleDescription(String input) {
            return execute("changeIssueTitleDescription", input);
        }

        @Override
        public String getEmbeds(String input) {
            return execute("getEmbeds", input);
        }

        @Override
        public String getAlias(String input) {
            return execute("getAlias", input);
        }

        @Override
        public String createPatchComment(String input) {
            return execute("createPatchComment", input);
        }

        @Override
        public String editPatchComment(String input) {
            return execute("editPatchComment", input);
        }

        @Override
        public String deletePatchComment(String input) {
            return execute("deletePatchComment", input);
        }

        @Override
        public String editIssueComment(String input) {
            return execute("editIssueComment", input);
        }

        @Override
        public String patchCommentReact(String input) {
            return execute("patchCommentReact", input);
        }

        @Override
        public String issueCommentReact(String input) {
            return execute("issueCommentReact", input);
        }

        public String execute(String method, String input) {
            var out = rad.executeCommandFromFile(wslBin, null, List.of(method, ExecUtil.escapeUnixShellArgument(input)));
            if (!RadAction.isSuccess(out)) {
                return "{\"ok\": false, \"msg\": \"error executing command with exit code:" + out.getExitCode() + " \"}";
            }
            return out.getStdout();
        }
    }
}
