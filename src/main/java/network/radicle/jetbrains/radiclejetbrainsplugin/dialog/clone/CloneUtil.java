package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.google.common.base.Strings;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloneUtil {
    private static final Logger logger = LoggerFactory.getLogger(CloneUtil.class);

    public static void doClone(@NotNull CheckoutProvider.Listener listener, @NotNull Project project, String url,
                               String projectName,String directoryPath) {

        var parent = Paths.get(directoryPath).toAbsolutePath().getParent();
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
        var directoryName = Paths.get(directoryPath).getFileName().toString();
        var parentDirectory = parent.toAbsolutePath().toString();
        File directory = new File(parentDirectory, directoryName);

        var clone = new RadClone(url,parentDirectory);
        var countDownLatch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var pr = new BasicAction(clone,project,countDownLatch).perform();
            if (pr.getExitCode() != 0) {
                return ;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                File oldDirectory = new File(parentDirectory,Strings.isNullOrEmpty(projectName) ?
                        getProjectName(pr.getStdoutLines(true)) : projectName);
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
                url, countDownLatch,  new AtomicBoolean(false));
        new Thread(ubt::queue).start();
    }

    /* Try to find the name of the project from output in case the user clone from a rad url */
    private static String getProjectName(List<String> lines) {
        var lastLine = lines.get(lines.size() - 1);
        String pattern = "[^./]*$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(lastLine);
        if (m.find()) {
            return m.group(0);
        }
        return "";
    }

}
