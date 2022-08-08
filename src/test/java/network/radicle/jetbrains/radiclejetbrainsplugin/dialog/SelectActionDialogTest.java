package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import network.radicle.jetbrains.radiclejetbrainsplugin.ActionsTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSync;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class SelectActionDialogTest extends ActionsTest {
    private RadicleSettingsHandler radicleSettingsHandler ;

    @Before
    public void init() {
        radicleSettingsHandler = new RadicleSettingsHandler();
    }

    @Test
    public void storeRadioSelectionTest() {
        SelectActionDialog dialog = new SelectActionDialog(super.getProject(),List.of(repository));
        assertThat(dialog.getRememberCheckBox().isSelected()).isFalse();
        assertThat(dialog.getYesRadio().isSelected()).isFalse();
        assertThat(dialog.getNoRadio().isSelected()).isFalse();

        dialog.getYesRadio().setSelected(true);
        dialog.doOKAction();

        var settings = radicleSettingsHandler.loadSettings();
        assertThat(settings.getRadSync()).isEqualTo(RadicleSettings.RadSyncType.ASK.val);

        dialog = new SelectActionDialog(super.getProject(),List.of(repository));
        dialog.getYesRadio().setSelected(true);
        dialog.getRememberCheckBox().setSelected(true);
        dialog.doOKAction();

        settings = radicleSettingsHandler.loadSettings();
        assertThat(settings.getRadSync()).isEqualTo(RadicleSettings.RadSyncType.YES.val);

        dialog = new SelectActionDialog(super.getProject(),List.of(repository));
        dialog.getNoRadio().setSelected(true);
        dialog.getRememberCheckBox().setSelected(true);
        dialog.doOKAction();

        settings = radicleSettingsHandler.loadSettings();
        assertThat(settings.getRadSync()).isEqualTo(RadicleSettings.RadSyncType.NO.val);
    }

    @Test
    public void checkRadSync() throws InterruptedException {
        SelectActionDialog dialog = new SelectActionDialog(super.getProject(),List.of(repository));
        assertThat(dialog.getRememberCheckBox().isSelected()).isFalse();
        assertThat(dialog.getYesRadio().isSelected()).isFalse();
        assertThat(dialog.getNoRadio().isSelected()).isFalse();

        /* run rad sync after yes selection */
        dialog.getYesRadio().setSelected(true);
        dialog.doOKAction();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        ActionsTest.assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("sync");

        var not = notificationsQueue.poll(10,TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadSync().getNotificationSuccessMessage());
    }

}
