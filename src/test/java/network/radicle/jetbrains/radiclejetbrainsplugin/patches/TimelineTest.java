package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.openapi.vcs.changes.Change;
import git4idea.GitCommit;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.TimelineComponent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.swing.JPanel;
import javax.swing.JComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TimelineTest extends AbstractIT {
    private static final String AUTHOR = "did:key:testAuthor";
    private static final String DUMMY_COMMENT = "Hello";
    private TimelineComponent patchEditorComponent;
    private RadPatch patch;

    @Before
    public void beforeTest() {
        patch = createPatch();
        var gitRepoManager = GitRepositoryManager.getInstance(getProject());
        var repos = gitRepoManager.getRepositories();
        patch.repo = repos.get(0);
        var patchModel = new SingleValueModel<>(patch);
        patchEditorComponent = new TimelineComponent(patchModel);
        patchEditorComponent.create();
    }

    @Test
    public void testHeader() {
        var titleEditor = patchEditorComponent.getHeaderTitle();
        assertThat(titleEditor.getText()).contains(patch.title);
        assertThat(titleEditor.getText()).contains(patch.id);
    }

    @Test
    public void testDescSection() {
        var descSection = patchEditorComponent.getComponentsFactory().getDescSection();
        var elements = findElement((JPanel) descSection, new BaseHtmlEditorPane(), new ArrayList<>());
        var timeline = "";
        for (var el : elements) {
            timeline += ((BaseHtmlEditorPane) el).getText();
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
        patchEditorComponent.createComment(DUMMY_COMMENT);
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("comment " + patch.id + " --message '" + DUMMY_COMMENT + "'");
    }

    public List<JComponent> findElement(JPanel panel, JComponent el, ArrayList<JComponent> components) {
        for (var element : panel.getComponents()) {
            if (element instanceof JPanel) {
                findElement((JPanel) element, el, components);
            } else {
                if (element.getClass().equals(el.getClass())) {
                    components.add((JComponent) element);
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
