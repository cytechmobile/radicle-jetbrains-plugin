package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadClone;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloneUtil {
    private static final Logger logger = LoggerFactory.getLogger(CloneUtil.class);

    public static void doClone(@NotNull CheckoutProvider.Listener listener, @NotNull Project project, CloneProject clPr) {

        var parent = Paths.get(clPr.directory()).toAbsolutePath().getParent();
        var destinationValidation = CloneDvcsValidationUtils.createDestination(parent.toString());
        if (destinationValidation != null) {
            BasicAction.showErrorNotification(project, RadicleBundle.message("cloneFailed"),
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
            BasicAction.showErrorNotification(project, RadicleBundle.message("cloneFailed"),
                    RadicleBundle.message("destinationDoesntExist"));
            logger.error("Clone Failed. Destination doesn't exist");
            return ;
        }
        var directoryName = Paths.get(clPr.directory()).getFileName().toString();
        var parentDirectory = parent.toAbsolutePath().toString();
        File directory = new File(parentDirectory, directoryName);

        var clone = new RadClone(clPr.url(), parentDirectory);
        var countDownLatch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var pr = new BasicAction(clone,project,countDownLatch).perform();
            if (pr.getExitCode() != 0) {
                return ;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                File oldDirectory = new File(parentDirectory, clPr.projectName(pr.getStdoutLines(true)));
                var success = oldDirectory.renameTo(directory);
                if (success) {
                    listener.directoryCheckedOut(directory,null);
                } else {
                    listener.directoryCheckedOut(oldDirectory,null);
                }
                listener.checkoutCompleted();
            });
        });
        var ubt = new UpdateBackgroundTask(project, RadicleBundle.message("cloningProcess") +
                clPr.url(), countDownLatch,  new AtomicBoolean(false));
        new Thread(ubt::queue).start();
    }
}
