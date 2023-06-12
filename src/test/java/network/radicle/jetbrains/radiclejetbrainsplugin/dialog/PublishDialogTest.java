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
    public void testDialogWithRadInitializedPr() throws InterruptedException {
        secondRepo = GitTestUtil.createGitRepository(super.getProject(), remoteRepoPath1);
        var publishDialog = new PublishDialog(List.of(secondRepo), super.getProject());
        assertThat(publishDialog.getProjectSelect().isVisible()).isTrue();
        assertThat(publishDialog.getProjectNameLabel().isVisible()).isTrue();
        assertThat(publishDialog.getSeedNodeSelect().isVisible()).isTrue();
        assertThat(publishDialog.getSeedNodeLabel().isVisible()).isTrue();

        publishDialog.getSeedNodeSelect().setSelectedIndex(0);
        Thread.sleep(100);
        publishDialog.doOKAction();

         var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
         assertCmd(cmd);
         assertThat(cmd.getCommandLineString()).contains("init");

         cmd = radStub.commands.poll(100, TimeUnit.MILLISECONDS);
         assertThat(cmd).isNull();

         var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
         assertThat(not).isNotNull();
         assertThat(not.getContent()).isEqualTo(RadicleBundle.message("radNotification_Init"));
         removeRemoteRadUrl(secondRepo);
    }

    @Test
    public void testDialogWithMultipleRepositories() {
        secondRepo = GitTestUtil.createGitRepository(super.getProject(), remoteRepoPath1);
        initializeProject(firstRepo);
        removeRemoteRadUrl(secondRepo);

        var publishDialog = new PublishDialog(List.of(firstRepo, secondRepo), super.getProject());
        assertThat(publishDialog.getProjectSelect().isVisible()).isTrue();
        assertThat(publishDialog.getProjectNameLabel().isVisible()).isTrue();
        assertThat(publishDialog.getSeedNodeSelect().isVisible()).isTrue();
        assertThat(publishDialog.getSeedNodeLabel().isVisible()).isTrue();
    }

    @Test
    public void testDialogWithoutRadInitializedPr() throws InterruptedException {
        secondRepo = GitTestUtil.createGitRepository(super.getProject(), remoteRepoPath1);
        removeRemoteRadUrl(secondRepo);
        var publishDialog = new PublishDialog(List.of(secondRepo), super.getProject());

        assertThat(publishDialog.getProjectSelect().isVisible()).isTrue();
        assertThat(publishDialog.getProjectNameLabel().isVisible()).isTrue();
        assertThat(publishDialog.getSeedNodeSelect().isVisible()).isTrue();
        assertThat(publishDialog.getSeedNodeLabel().isVisible()).isTrue();

        publishDialog.getSeedNodeSelect().setSelectedIndex(0);
        publishDialog.getBranchField().setText(BRANCHNAME);
        publishDialog.getNameField().setText(NAME);
        publishDialog.getDescriptionField().setText(DESCRIPTION);

        Thread.sleep(1000);
        publishDialog.doOKAction();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("init --name " + NAME + " --description " + DESCRIPTION +
                " --default-branch " + BRANCHNAME + " --no-confirm");

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("radNotification_Init"));
    }

}
