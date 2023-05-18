package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TrackDialogTest extends AbstractIT {

    private static final String PEER_ID = "zhtfhqjrk";
    private static final String ALIAS = "alias";
    private static final String REPO_ID = "repo_id";

    @Test
    public void testFieldsVisibility() {
        var trackDialog = new TrackDialog(getProject());
        var selectedAction = ((RadTrack.RadTrackType) Objects.requireNonNull(trackDialog.getTrackActionSelect().getSelectedItem())).name;
        assertThat(selectedAction).isEqualTo(RadTrack.RadTrackType.TRACK_PEER.name);
        testFields(trackDialog, true);

        trackDialog.getTrackActionSelect().setSelectedIndex(1);
        selectedAction = ((RadTrack.RadTrackType) trackDialog.getTrackActionSelect().getSelectedItem()).name;
        assertThat(selectedAction).isEqualTo(RadTrack.RadTrackType.TRACK_REPOSITORY.name);
        testFields(trackDialog, false);
    }

    private void testFields(TrackDialog trackDialog, boolean isVisible) {
        assertThat(trackDialog.getTrackActionSelect().isVisible()).isTrue();

        assertThat(trackDialog.getPeerIdLabel().isVisible()).isEqualTo(isVisible);
        assertThat(trackDialog.getPeerIdField().isVisible()).isEqualTo(isVisible);
        assertThat(trackDialog.getAlliasLabel().isVisible()).isEqualTo(isVisible);
        assertThat(trackDialog.getAliasField().isVisible()).isEqualTo(isVisible);

        assertThat(trackDialog.getRepositoryIdLabel().isVisible()).isEqualTo(!isVisible);
        assertThat(trackDialog.getRepositoryField().isVisible()).isEqualTo(!isVisible);
        assertThat(trackDialog.getScopeLabel().isVisible()).isEqualTo(!isVisible);
        assertThat(trackDialog.getScopeSelect().isVisible()).isEqualTo(!isVisible);
    }

    @Test
    public void testPeer() throws InterruptedException {
        var trackDialog = new TrackDialog(getProject());
        trackDialog.getPeerIdField().setText(PEER_ID);
        trackDialog.doOKAction();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("track " + PEER_ID);
        assertThat(cmd.getCommandLineString()).doesNotContain("--alias");

        trackDialog.getAliasField().setText(ALIAS);
        trackDialog.doOKAction();
        cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("track " + PEER_ID + " --alias " + ALIAS);

    }

    @Test
    public void testRepository() throws InterruptedException {
        var trackDialog = new TrackDialog(getProject());
        trackDialog.getTrackActionSelect().setSelectedIndex(1);
        trackDialog.getRepositoryField().setText(REPO_ID);
        trackDialog.doOKAction();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("track " + REPO_ID);
        assertThat(cmd.getCommandLineString()).doesNotContain("--scope");

        trackDialog.getScopeSelect().setSelectedIndex(1);
        trackDialog.doOKAction();

        cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        var selected = ((RadTrack.Scope) Objects.requireNonNull(trackDialog.getScopeSelect().getSelectedItem())).name.toLowerCase();
        assertThat(cmd.getCommandLineString()).contains("track " + REPO_ID + " --scope " + selected);

    }


}
