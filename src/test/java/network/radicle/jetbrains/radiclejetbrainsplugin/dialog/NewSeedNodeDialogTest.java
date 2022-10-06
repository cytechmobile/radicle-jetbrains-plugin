package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class NewSeedNodeDialogTest extends AbstractIT {

    @Test
    public void testValidIpAddress() {
        var ip = "192.168.1.1";
        var port = "8080";
        var addSeedNodeDialog = new AddSeedNodeDialog(List.of());
        var nodeField = addSeedNodeDialog.getNodeField();
        var portField = addSeedNodeDialog.getPortField();
        nodeField.setText(ip);
        portField.setText(port);
        addSeedNodeDialog.doOKAction();
        var res = addSeedNodeDialog.getErrorField().getText();
        assertThat(res).isEmpty();
    }

    @Test
    public void testValidDomainName() {
        var domain = "pine.radicle.garden";
        var port = "8080";
        var addSeedNodeDialog = new AddSeedNodeDialog(List.of());
        var nodeField = addSeedNodeDialog.getNodeField();
        var portField = addSeedNodeDialog.getPortField();
        nodeField.setText(domain);
        portField.setText(port);
        addSeedNodeDialog.doOKAction();
        var res = addSeedNodeDialog.getErrorField().getText();
        assertThat(res).isEmpty();
    }

    @Test
    public void testInvalidSeedNode() {
        var domain = "test";
        var port = "8080";
        var addSeedNodeDialog = new AddSeedNodeDialog(List.of());
        var nodeField = addSeedNodeDialog.getNodeField();
        var portField = addSeedNodeDialog.getPortField();
        nodeField.setText(domain);
        portField.setText(port);
        addSeedNodeDialog.doOKAction();
        var res = addSeedNodeDialog.getErrorField().getText();
        assertThat(res).isNotEmpty();
        assertThat(res).isEqualTo(RadicleBundle.message("invalidSeedNode"));
    }

    @Test
    public void testInvalidPort() {
        var domain = "pine.radicle.garden";
        var port = "test";
        var addSeedNodeDialog = new AddSeedNodeDialog(List.of());
        var nodeField = addSeedNodeDialog.getNodeField();
        var portField = addSeedNodeDialog.getPortField();
        nodeField.setText(domain);
        portField.setText(port);
        addSeedNodeDialog.doOKAction();
        var res = addSeedNodeDialog.getErrorField().getText();
        assertThat(res).isNotEmpty();
        assertThat(res).isEqualTo(RadicleBundle.message("invalidPort"));
    }

    @Test
    public void testSeedNodeExists() {
        var domain = "pine.radicle.garden";
        var port = "8080";
        var loadedSeedNodes = List.of(new SeedNode(domain,port));
        var addSeedNodeDialog = new AddSeedNodeDialog(loadedSeedNodes);
        var nodeField = addSeedNodeDialog.getNodeField();
        var portField = addSeedNodeDialog.getPortField();
        nodeField.setText(domain);
        portField.setText(port);
        addSeedNodeDialog.doOKAction();
        var res = addSeedNodeDialog.getErrorField().getText();
        assertThat(res).isNotEmpty();
        assertThat(res).isEqualTo(RadicleBundle.message("seedNodeExists"));
    }

}
