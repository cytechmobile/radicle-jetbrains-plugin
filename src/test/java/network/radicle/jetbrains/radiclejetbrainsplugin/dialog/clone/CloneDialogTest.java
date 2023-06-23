package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.openapi.vcs.VcsKey;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class CloneDialogTest extends AbstractIT {

    private RadicleProjectSettingsHandler radicleProjectSettingsHandler;
    private CloneRadDialog cloneDialog;
    private RadicleProjectApi api;
    private CloseableHttpClient httpClient;
    private CloseableHttpResponse httpResponse;
    private StatusLine statusLine;

    @Before
    public void beforeTest() {
        radicleProjectSettingsHandler = new RadicleProjectSettingsHandler(getProject());
        radicleProjectSettingsHandler.savePath("");
        api = replaceApiService();
        httpClient = api.getClient();
        httpResponse = mock(CloseableHttpResponse.class);
        statusLine = mock(StatusLine.class);
        cloneDialog = new CloneRadDialog(myProject);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    public void testCloneDialogExtName() {
        var cloneDialogExt = new CloneRadDialogExtension();
        assertThat(cloneDialogExt.getName()).isEqualTo(RadicleBundle.message("radicle"));
    }

    @Test
    public void testActiveProfile() throws InterruptedException {
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("radCliPathMissing"));
        radicleProjectSettingsHandler.savePath(RAD_PATH);
        cloneDialog = new CloneRadDialog(super.myProject);
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("self");
    }

    @Test
    public void testDefaultSeedNodes() {
        var seedNodeModel = cloneDialog.seedNodeComboBox.getModel();
        var settingsSeedNode = radicleProjectSettingsHandler.loadSettings().getSeedNode();
        assertThat(seedNodeModel.getElementAt(0).url).isEqualTo(settingsSeedNode.url);
    }

    @Test
    public void testErrorRequest() throws IOException, InterruptedException {
        when(statusLine.getStatusCode()).thenReturn(400);
        when(httpClient.execute(any())).thenReturn(httpResponse);

        cloneDialog.seedNodeComboBox.setSelectedIndex(0);
        cloneDialog.loadProjects();

        notificationsQueue.take();
        Thread.sleep(2000);
        assertThat(cloneDialog.errorMsg.isVisible()).isTrue();
        assertThat(cloneDialog.errorMsg.getText()).contains(RadicleBundle.message("httpRequestErrorTitle"));
        assertThat(cloneDialog.errorMsg.getText()).contains(RadicleBundle.message("httpRequestErrorDesc"));
    }

    @Test
    public void testSuccessRequest() throws IOException {
        StringEntity se = new StringEntity("");
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        when(httpResponse.getEntity()).thenReturn(se);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        cloneDialog.seedNodeComboBox.setSelectedIndex(0);
        cloneDialog.loadProjects();
        assertThat(cloneDialog.page).isEqualTo(0);
        var loadMoreListener = cloneDialog.new LoadButtonListener();
        loadMoreListener.actionPerformed(null);
        assertThat(cloneDialog.page).isEqualTo(1);
    }

    @Test
    public void successCloneTest() throws InterruptedException {
        radicleProjectSettingsHandler.savePath("");
        var radProject = new RadProject("hnrk81ky87cii8h68nedkej991c5dspazi9xy", "testName", "Test", "main");
        cloneDialog.projectModel.addElement(radProject);
        cloneDialog.radProjectJBList.setSelectedIndex(0);
        cloneDialog.doClone(new CheckoutProvider());
        /* remove self profile first */
        radStub.commands.poll(100, TimeUnit.MILLISECONDS);
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("radCliPathMissing"));
        radicleProjectSettingsHandler.savePath(RAD_PATH);
        cloneDialog.doClone(new CheckoutProvider());
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("clone " + radProject.id);
        cloneDialog.radProjectJBList.remove(0);
    }

    @Test
    public void errorCloneTest() throws InterruptedException {
        var radProject = new RadProject("hnr", "testName", "Test", "main");
        cloneDialog.projectModel.addElement(radProject);
        cloneDialog.radProjectJBList.setSelectedIndex(0);
        cloneDialog.doClone(new CheckoutProvider());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
    }

    public static class CheckoutProvider implements com.intellij.openapi.vcs.CheckoutProvider.Listener {
        @Override
        public void directoryCheckedOut(File directory, VcsKey vcs) {
            assertThat(directory.getName()).contains("testName");
        }

        @Override
        public void checkoutCompleted() { }
    }
}
