package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.dvcs.DvcsRememberedInputs;
import com.intellij.dvcs.ui.DvcsCloneDialogComponent;
import com.intellij.openapi.vcs.checkout.CompositeCheckoutListener;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogComponentStateListener;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.util.ui.UIUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class RepositoryTest extends AbstractIT {

    private DvcsCloneDialogComponent radCheckoutComponent;

    @Before
    public void beforeTest() {
        radCheckoutComponent = new RadCheckoutProvider.VcsCloneComponentExt(super.myProject, "",
                new DvcsRememberedInputs(), new MyListener());
    }

    @Test
    public void testClone() throws InterruptedException {
        var urlField = UIUtil.findComponentOfType(radCheckoutComponent.getView(), TextFieldWithHistory.class);
        urlField.setText("rad:git123");
        radStub.commands.clear();
        radCheckoutComponent.doClone(new CompositeCheckoutListener(super.myProject));
        executeUiTasks();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("rad --version");
        var cmd1 = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd1).isNotNull();
        assertCmd(cmd1);
        assertThat(cmd1.getCommandLineString()).contains("clone rad:git123");
    }

    public class MyListener implements VcsCloneDialogComponentStateListener {
        @Override
        public void onListItemChanged() { }

        @Override
        public void onOkActionEnabled(boolean b) { }

        @Override
        public void onOkActionNameChanged(@Nls @NotNull String s) { }
    }

}
