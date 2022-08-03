package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateBackgroundTask extends Task.Backgroundable {
    private static final Logger logger = Logger.getInstance(UpdateBackgroundTask.class);
    protected CountDownLatch updateCountDownLatch;
    protected AtomicBoolean executingFlag;

    public UpdateBackgroundTask(
            Project project, String title,
            CountDownLatch updatingLatch, AtomicBoolean executing) {
        super(project, title, false, PerformInBackgroundOption.ALWAYS_BACKGROUND);
        this.updateCountDownLatch = updatingLatch;
        this.executingFlag = executing;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            updateCountDownLatch.await(10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("error awaiting update latch!", e);
        }
        executingFlag.set(false);
    }
}
