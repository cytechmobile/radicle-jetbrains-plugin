package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadClone;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class CloneUtil {
    private static final Logger logger = LoggerFactory.getLogger(CloneUtil.class);

    public static void doClone(@NotNull CheckoutProvider.Listener listener, @NotNull Project project, CloneProject clPr) {
        String tmpFolderPath;
        var parent = Paths.get(clPr.directory());
        var destinationValidation = CloneDvcsValidationUtils.createDestination(parent.toString());
        if (destinationValidation != null) {
            RadAction.showErrorNotification(project, RadicleBundle.message("cloneFailed"),
                    RadicleBundle.message("directoryError"));
            logger.error("Clone Failed. Unable to create destination directory");
            return ;
        }

        var lfs = LocalFileSystem.getInstance();
        var destinationParent = lfs.findFileByIoFile(parent.toFile());
        if (destinationParent == null) {
            destinationParent = lfs.refreshAndFindFileByIoFile(parent.toFile());
        }
        if (destinationParent == null) {
            RadAction.showErrorNotification(project, RadicleBundle.message("cloneFailed"),
                    RadicleBundle.message("destinationDoesntExist"));
            logger.error("Clone Failed. Destination doesn't exist");
            return ;
        }

        try {
            var tmpFolder = Files.createTempDirectory("project" + UUID.randomUUID());
            tmpFolderPath = tmpFolder.toAbsolutePath().toString();
        } catch (Exception e) {
            RadAction.showErrorNotification(project, RadicleBundle.message("cloneFailed"),
                    RadicleBundle.message("tempDirError"));
            logger.error("Unable to create temp directory");
            return ;
        }

        final var countDownLatch = new CountDownLatch(1);
        String finalTmpFolderPath = tmpFolderPath;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final var clone = new RadClone(clPr.url(), tmpFolderPath);
            var pr = clone.perform(countDownLatch);
            if (pr.getExitCode() != 0) {
                return ;
            }
            var tmpFiles = new File(finalTmpFolderPath);
            var folders = tmpFiles.list();
            if (folders == null || folders.length == 0) {
                try {
                    FileUtils.deleteDirectory(new File(finalTmpFolderPath));
                } catch (IOException e) {
                    logger.warn("unable to delete temp dir:", e);
                }
                return;
            }
            var projectName = folders[0];
            var tmpDirectory = new File(finalTmpFolderPath, projectName);
            var selectedDirectory = new File(parent.toAbsolutePath().toString());
            try {
                FileUtils.copyDirectory(tmpDirectory, selectedDirectory);
                FileUtils.deleteDirectory(new File(finalTmpFolderPath));
                listener.directoryCheckedOut(selectedDirectory, null);
                listener.checkoutCompleted();
            } catch (Exception e) {
                logger.warn("unable to copy / delete temp dir:", e);
            }
        });
        var ubt = new UpdateBackgroundTask(project, RadicleBundle.message("cloningProcess") +
                clPr.url(), countDownLatch);
        ubt.startInThread();
    }
}
