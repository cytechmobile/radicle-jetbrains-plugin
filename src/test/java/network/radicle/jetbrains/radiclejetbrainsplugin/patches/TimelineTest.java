package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.UIUtil;
import git4idea.GitCommit;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadStub;
import network.radicle.jetbrains.radiclejetbrainsplugin.SettingsStub;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.IdentityDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.TimelineComponent;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.wilson.xml.MinML.change;

@RunWith(JUnit4.class)
public class TimelineTest extends AbstractIT {
    private static final Logger logger = LoggerFactory.getLogger(TimelineTest.class);
    private static final String AUTHOR = "did:key:testAuthor";
    private static String dummyComment = "Hello";
    private TimelineComponent patchEditorComponent;
    private RadPatch patch;
    private RadicleProjectApi api;

    @Before
    public void beforeTest() {
        api = replaceApiService();
        patch = createPatch();
        patch.repo = firstRepo;
        var patchModel = new SingleValueModel<>(patch);
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
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
    public void testChangeTitle() {
        final var titlePanel = patchEditorComponent.getHeaderPanel();
        var editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();

        final var ef = UIUtil.findComponentOfType(titlePanel, EditorTextField.class);
        assertThat(ef.getText()).isEqualTo(patch.title);

        //UIUtil.markAsShowing((JComponent) ef.getParent(), true);
        //matching UiUtil IS_SHOWING key
        ((JComponent)ef.getParent()).putClientProperty(Key.findKeyByName("Component.isShowing"), Boolean.TRUE);
        assertThat(UIUtil.isShowing(ef.getParent(), false)).isTrue();
        for (var hl : ef.getParent().getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(ef, 0, ef, ef.getParent(), HierarchyEvent.SHOWING_CHANGED));
        }
        executeUiTasks();

        final var editedTitle = "Edited title to " + UUID.randomUUID();
        ef.setText(editedTitle);
        final var prBtns = UIUtil.findComponentsOfType(titlePanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        final var prBtn = prBtns.get(1);
        /* unable to stub radWeb action , the problem is in unlockIdentity function line : 112 (projectSettings.getPassword(radDetails.nodeId);)
        If I replace this with a random string then it works */
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

    public <T> List<T> findElements(JPanel panel, Class<T> el, List<T> components) {
        for (var element : panel.getComponents()) {
            logger.warn("looking for {} at element: {}", el, element);
            if (el.isAssignableFrom(element.getClass())) {
                logger.warn("looking for {} found element: {}", el, element);
                components.add((T) element);
            } else if (element instanceof JPanel) {
                findElements((JPanel) element, el, components);
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
