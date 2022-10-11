package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.ui.VcsCloneComponent;
import com.intellij.ui.components.JBTextField;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class RepositoryTest extends AbstractIT {

   private VcsCloneComponent radUrl;

    @Before
    public void beforeTest() {
        radUrl = new RadCheckoutProvider(). new RadUrl(super.myProject);
    }

    @Test
    public void testClone() throws InterruptedException {
        var panel = (JPanel) radUrl.getView().getComponents()[0];
        var components = panel.getComponents();
        var radUrlField = (JBTextField) components[1];
        radUrlField.setText("rad:git123");
        radUrl.doClone(new CheckoutProvider());
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(wsl);
        } else {
            assertThat(cmd.getExePath()).isEqualTo(radPath);
        }
        assertThat(cmd.getCommandLineString()).contains("clone rad:git123");
    }

    private class CheckoutProvider implements com.intellij.openapi.vcs.CheckoutProvider.Listener {
        @Override
        public void directoryCheckedOut(File directory, VcsKey vcs) {
        }

        @Override
        public void checkoutCompleted() {}
    }
}
