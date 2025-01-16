package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.GitTestUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class PublishDialogTest extends AbstractIT {
    private static final String BRANCHNAME = "main";
    private static final String NAME = "testName";
    private static final String DESCRIPTION = "testDesc";

    @Test
    public void testDialogWithMultipleRepositories() throws Exception {
        secondRepo = GitTestUtil.createGitRepository(super.getProject(), remoteRepoPath1);
        initializeProject(firstRepo);
        removeRemoteRadUrl(secondRepo);

        var publishDialog = new PublishDialog(List.of(firstRepo, secondRepo), super.getProject());
        assertThat(publishDialog.getProjectSelect().isVisible()).isTrue();
        assertThat(publishDialog.getProjectNameLabel().isVisible()).isTrue();
    }

    @Test
    public void testDialogWithoutRadInitializedPr() throws Exception {
        secondRepo = GitTestUtil.createGitRepository(super.getProject(), remoteRepoPath1);
        removeRemoteRadUrl(secondRepo);
        var publishDialog = new PublishDialog(List.of(secondRepo), super.getProject());

        assertThat(publishDialog.getProjectSelect().isVisible()).isTrue();
        assertThat(publishDialog.getProjectNameLabel().isVisible()).isTrue();

        publishDialog.getVisibilitySelect().setSelectedIndex(0);
        publishDialog.getBranchField().setText(BRANCHNAME);
        publishDialog.getNameField().setText(NAME);
        publishDialog.getDescriptionField().setText(DESCRIPTION);

        executeUiTasks();
        publishDialog.doOKAction();
        executeUiTasks();

        var cmd = radStub.commands.poll(100, TimeUnit.MILLISECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("init --name " + NAME + " --description " + DESCRIPTION +
                " --default-branch " + BRANCHNAME + " --public --no-confirm");

        var not = notificationsQueue.poll(100, TimeUnit.MILLISECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("radNotification_Init"));

        publishDialog.getVisibilitySelect().setSelectedIndex(1);
        executeUiTasks();
        publishDialog.doOKAction();
        executeUiTasks();
        cmd = radStub.commands.poll(100, TimeUnit.MILLISECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("init --name " + NAME + " --description " + DESCRIPTION +
                " --default-branch " + BRANCHNAME + " --private --no-confirm");
    }

}
