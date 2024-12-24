package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.google.common.base.Strings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jnr.ffi.LibraryLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

public class RadicleNativeService {
    private static final Logger logger = Logger.getInstance(RadicleNativeService.class);

    public static JavaRad javaRad;
    public static Path libFile;

    private final Project project;

    public RadicleNativeService(Project project) {
        this.project = project;
        load();
    }

    public String radHome() {
        if (javaRad == null) {
            return null;
        }
        return javaRad.radHome("jchrist");
    }

    public JRadResponse editIssueTitleDescription(String repoId, String issueId, String title, String description) {
        if (javaRad == null) {
            return new JRadResponse(false, "native service not loaded");
        }

        try {
            var json = RadicleCliService.MAPPER.writeValueAsString(
                    Map.of("repo_id", repoId,
                            "issue_id", issueId,
                            "title", Strings.nullToEmpty(title),
                            "description", Strings.nullToEmpty(description)));
            var res = javaRad.changeIssueTitle(json);
            var resp = RadicleCliService.MAPPER.readValue(res, JRadResponse.class);
            if (resp == null || !resp.ok) {
                logger.warn("error response from native service: " + resp);
            }
            return resp;
        } catch (Exception e) {
            logger.error("Error changing issue title", e);
            return new JRadResponse(false, e.getMessage());
        }
    }

    public static Path createTempFileFromJar(final String library) throws Exception {
        // create temp file
        var tempDir = Files.createTempDirectory("radicle-jetbrains-plugin-native-" + System.currentTimeMillis());
        var tempFile = Files.createFile(tempDir.resolve(library));
        try (var is = RadicleNativeService.class.getResourceAsStream("/META-INF/jrad/" + library)) {
            Files.copy(Objects.requireNonNull(is), tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.out.println("error opening library resource stream: " + e);
        } finally {
            // set folder to be deleted after VM shuts down
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
        }
        return tempFile;
    }

    public static void load() {
        if (javaRad != null) {
            return;
        }
        try {
            var libName = System.mapLibraryName("jrad");
            libFile = createTempFileFromJar(libName);
            javaRad = LibraryLoader.create(JavaRad.class).search(Paths.get("./jrad/target/debug").toAbsolutePath().normalize().toString())
                    .search(libFile == null ? "." : libFile.toAbsolutePath().getParent().toString())
                    .load("jrad");
        } catch (Throwable t) {
            logger.error("Error loading native library", t);
        }
    }

    public record JRadResponse(boolean ok, String msg) { }

    public interface JavaRad {
        String radHome(String input);
        String changeIssueTitle(String input);
    }
}
