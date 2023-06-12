package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.InlineIconButton;
import git4idea.GitCommit;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.TimelineComponent;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TimelineTest extends AbstractIT {
    private static final String AUTHOR = "did:key:testAuthor";
    private static String dummyComment = "Hello";
    private TimelineComponent patchEditorComponent;
    private RadPatch patch;
    private RadicleProjectApi api;

    @Before
    public void beforeTest() {
        api = replaceApiService();
        patch = createPatch();
        var gitRepoManager = GitRepositoryManager.getInstance(getProject());
        var repos = gitRepoManager.getRepositories();
        patch.repo = repos.get(0);
        var patchModel = new SingleValueModel<>(patch);
        patchEditorComponent = new TimelineComponent(patchModel, null);
        patchEditorComponent.create();
    }

    @Test
    public void testHeader() {
        var titleEditor = patchEditorComponent.getHeaderTitle();
        assertThat(titleEditor.getText()).contains(patch.title);
        assertThat(titleEditor.getText()).contains(patch.id);
    }

    @Test
    public void testChangeTitle() throws Exception {
        final var titlePanel = patchEditorComponent.getHeaderPanel();
        var btns = findElements(titlePanel, InlineIconButton.class, new ArrayList<>());
        assertThat(btns).hasSize(1);
        var editBtn = btns.get(0);
        //send event that we clicked edit
        //TODO fix
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        //editListener.mouseClicked(new MouseEvent(editBtn, 0, 0, 0, 0, 0, 1, false, 1));
        // now we should be able to see the BaseHtmlEditorPane in the header panel
        var efs = findElements(titlePanel, EditorTextField.class, new ArrayList<>());
        for (int i=0; i<10 && efs.size() <= 0; i++) {
            Thread.sleep(100);
            efs = findElements(titlePanel, EditorTextField.class, new ArrayList<>());
        }
        assertThat(efs).hasSize(1);
        var ef = efs.get(0);
        assertThat(ef.getText()).isEqualTo(patch.title);
        final var editedTitle = "Edited title to " + UUID.randomUUID();
        ef.setText(editedTitle);
        // title edited, now we need to find the primary button, which is expected to be a JOptionButton
        final var prBtns = findElements(titlePanel, JButton.class, new ArrayList<>());
        assertThat(prBtns).hasSize(1);
        final var prBtn = prBtns.get(0);
        prBtn.doClick();
    }

    @Test
    public void testDescSection() {
        var descSection = patchEditorComponent.getComponentsFactory().getDescSection();
        var elements = findElements((JPanel) descSection, BaseHtmlEditorPane.class, new ArrayList<>());
        var timeline = "";
        for (var el : elements) {
            timeline += el.getText();
        }
        assertThat(timeline).contains(patch.description);
        assertThat(timeline).contains(patch.author.id());
    }

    @Test
    public void testRevSection() throws InterruptedException {
        boolean waited = patchEditorComponent.getComponentsFactory().getLatch().await(3, TimeUnit.SECONDS);
        assertThat(waited).isTrue();
        var groupedCommits = patchEditorComponent.getComponentsFactory().getGroupedCommits();
        assertThat(groupedCommits.get(patch.revisions.get(0).id()).get(0)).isEqualTo(commitHistory.get(0));
        assertThat(groupedCommits.get(patch.revisions.get(1).id()).get(0)).isEqualTo(commitHistory.get(1));
    }

    @Test
    public void testComment() throws InterruptedException {
        //wait for rad self to finish
        radStub.commands.poll(10, TimeUnit.SECONDS);

        radicleProjectSettingsHandler = new RadicleProjectSettingsHandler(getProject());
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        patchEditorComponent.createComment(dummyComment);
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        if (SystemInfo.isWindows) {
            dummyComment = "'" + dummyComment + "'";
        }
        assertThat(cmd.getCommandLineString()).contains("comment " + patch.id + " --message " + dummyComment);
    }

    public <T> List<T> findElements(JPanel panel, Class<T> el, ArrayList<T> components) {
        for (var element : panel.getComponents()) {
            if (element instanceof JPanel) {
                findElements((JPanel) element, el, components);
            } else {
                if (el.isAssignableFrom(element.getClass())) {
                    components.add((T) element);
                }
            }
        }
        return components;
    }

    private RadPatch createPatch() {
        var firstCommit = commitHistory.get(0);
        var secondCommit = commitHistory.get(1);
        var firstDiscussion = createDiscussion("123", "123", "hello");
        var secondDiscussion = createDiscussion("321", "321", "hello back");
        var firstRev = createRevision("testRevision1", "testRevision1", firstCommit, firstDiscussion);
        var secondRev = createRevision("testRevision2", "testRevision1", secondCommit, secondDiscussion);
        var myPatch = new RadPatch("c5df12", "testPatch", new RadPatch.Author(AUTHOR), "testDesc",
                "testTarget", List.of("tag1", "tag2"), RadPatch.State.OPEN, List.of(firstRev, secondRev));
        myPatch.project = getProject();
        return myPatch;
    }

    private RadPatch.Revision createRevision(String id, String description, GitCommit commit,
                                             RadPatch.Discussion discussion) {
        var fistCommitChanges = (ArrayList) commit.getChanges();
        var firstChange = (Change) fistCommitChanges.get(0);
        var base = firstChange.getBeforeRevision().getRevisionNumber().asString();
        return new RadPatch.Revision(id, description, base, commit.getId().asString(),
                List.of(), List.of(), Instant.now(), List.of(discussion), List.of());
    }

    private RadPatch.Discussion createDiscussion(String id, String authorId, String body) {
        return new RadPatch.Discussion(id, new RadPatch.Author(authorId), body, Instant.now(), "", List.of());
    }

}
