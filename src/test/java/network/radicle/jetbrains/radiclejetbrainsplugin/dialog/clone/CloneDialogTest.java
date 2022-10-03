package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.VcsKey;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class CloneDialogTest extends AbstractIT {

    private RadicleSettingsHandler radicleSettingsHandler;
    private CloneRadDialog cloneDialog;
    private HttpClient httpClient;
    private HttpResponse httpResponse;
    private StatusLine statusLine;

    @Before
    public void beforeTest() {
        radicleSettingsHandler = new RadicleSettingsHandler();
        radicleSettingsHandler.savePath("");
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        statusLine = mock(StatusLine.class);
        cloneDialog = new CloneRadDialog(super.myProject,new ProjectApi(httpClient));
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
        radicleSettingsHandler.savePath(radPath);
        cloneDialog = new CloneRadDialog(super.myProject,new ProjectApi(httpClient));
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(wsl);
        } else {
            assertThat(cmd.getExePath()).isEqualTo(radPath);
        }
        assertThat(cmd.getCommandLineString()).contains("self --profile");
    }

    @Test
    public void testDefaultSeedNodes() {
       var seedNodeModel = (DefaultTableModel) cloneDialog.myDecorator.getTable().getModel();
       var loadedSeedNodes =  cloneDialog.myDecorator.getLoadedSeedNodes();
       assertThat(loadedSeedNodes).usingRecursiveComparison().isEqualTo(RadicleSettingsHandler.DEFAULT_SEED_NODES);
       assertThat(seedNodeModel.getRowCount()).isEqualTo(loadedSeedNodes.size());
    }

    @Test
    public void testErrorRequest() throws IOException, InterruptedException {
        when(statusLine.getStatusCode()).thenReturn(400);
        when(httpClient.execute(any())).thenReturn(httpResponse);

        cloneDialog.myDecorator.getTable().setRowSelectionInterval(0,0);
        var listener =  cloneDialog.new ListSelectionListener(CloneRadDialog.SelectionType.SEEDNODE);
        listener.loadProjects();

        notificationsQueue.take();
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("httpRequestErrorTitle"));
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("httpRequestErrorDesc"));
        assertThat(not).isNotNull();

    }

    @Test
    public void testSuccessRequest() throws IOException, InterruptedException {

        StringEntity se = new StringEntity("");
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        when(httpResponse.getEntity()).thenReturn(se);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpClient.execute(any())).thenReturn(httpResponse);

        cloneDialog.myDecorator.getTable().setRowSelectionInterval(0,0);
        var listener =  cloneDialog.new ListSelectionListener(CloneRadDialog.SelectionType.SEEDNODE);
        listener.loadProjects();

        assertThat(cloneDialog.page).isEqualTo(0);
        var loadMoreListener = cloneDialog.new LoadButtonListener();
        loadMoreListener.actionPerformed(null);
        assertThat(cloneDialog.page).isEqualTo(1);
    }

    @Test
    public void successCloneTest() throws InterruptedException {
        radicleSettingsHandler.savePath("");
        var radProject = new RadProject("hnrk81ky87cii8h68nedkej991c5dspazi9xy","testName","Test","rad:hnrk81ky87cii8h68nedkej991c5dspazi9xy");
        cloneDialog.projectModel.addElement(radProject);
        cloneDialog.radProjectJBList.setSelectedIndex(0);
        cloneDialog.doClone(new CheckoutProvider());
        /* remove self profile first */
        radStub.commands.poll(10, TimeUnit.SECONDS);
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("radCliPathMissing"));
        radicleSettingsHandler.savePath(radPath);
        cloneDialog.doClone(new CheckoutProvider());
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(wsl);
        } else {
            assertThat(cmd.getExePath()).isEqualTo(radPath);
        }
        assertThat(cmd.getCommandLineString()).contains("clone " + radProject.radUrl);
        cloneDialog.radProjectJBList.remove(0);
    }

    @Test
    public void errorCloneTest() throws InterruptedException {
        var radProject = new RadProject("hnr","testName","Test","rad:ooo");
        cloneDialog.projectModel.addElement(radProject);
        cloneDialog.radProjectJBList.setSelectedIndex(0);
        cloneDialog.doClone(new CheckoutProvider());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
    }

    private class CheckoutProvider implements com.intellij.openapi.vcs.CheckoutProvider.Listener {
        @Override
        public void directoryCheckedOut(File directory, VcsKey vcs) {
           assertThat(directory.getName()).contains("testName");
        }

        @Override
        public void checkoutCompleted() {}
    }
}
