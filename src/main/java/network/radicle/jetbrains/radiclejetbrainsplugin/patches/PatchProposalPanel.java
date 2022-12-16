package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.diff.editor.DiffEditorTabFilesManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.DiffPreview;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import javax.swing.*;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

public class PatchProposalPanel {
    private final RadicleSettings settings;
    private final Project project;

    public PatchProposalPanel(Project pr) {
        this.project  = pr;
        this.settings = new RadicleSettingsHandler().loadSettings();
    }

    public JComponent createViewPatchProposalPanel(PatchTabController controller, Project pr) {
        var uiDisposable = Disposer.newDisposable();
        var infoComponent = new JPanel();
        infoComponent.add(new JLabel("INFO COMPONENT\nTODO: IMPLEMENT"));

        var commitComponent = new JPanel();
        commitComponent.add(new JLabel("Commit Component\nTODO: IMPLEMENT"));

        var filesComponent = createFilesComponent(pr);

        var tabInfo = new TabInfo(infoComponent);
        tabInfo.setText(RadicleBundle.message("info"));
        tabInfo.setSideComponent(createReturnToListSideComponent(controller));

        var filesInfo = new TabInfo(filesComponent);
        filesInfo.setText(RadicleBundle.message("files"));
        filesInfo.setSideComponent(createReturnToListSideComponent(controller));

        var commitInfo = new TabInfo(commitComponent);
        commitInfo.setText(RadicleBundle.message("commits"));
        commitInfo.setSideComponent(createReturnToListSideComponent(controller));

        var tabs = new SingleHeightTabs(null,uiDisposable);
        tabs.addTab(tabInfo);
        tabs.addTab(commitInfo);
        tabs.addTab(filesInfo);
        return tabs;
    }

    private JComponent createFilesComponent(Project pr) {
        var panel = new BorderLayoutPanel().withBackground(UIUtil.getListBackground());

        var changes = createChangesTree();
        panel.add(changes);

        return panel;
    }

    private JComponent createChangesTree() {
        var c1 = new ContentRevision(){

            @Override
            public @Nullable String getContent() throws VcsException {
                return "Test";
            }

            @Override
            public @NotNull FilePath getFile() {
                return new FilePath() {
                    @Override
                    public @Nullable VirtualFile getVirtualFile() {
                        return null;
                    }

                    @Override
                    public @Nullable VirtualFile getVirtualFileParent() {
                        return null;
                    }

                    @Override
                    public @NotNull File getIOFile() {
                        return null;
                    }

                    @Override
                    public @NlsSafe @NotNull String getName() {
                        return "test21.txt";
                    }

                    @Override
                    public @NlsSafe @NotNull String getPresentableUrl() {
                        return null;
                    }

                    @Override
                    public @Nullable Document getDocument() {
                        return null;
                    }

                    @Override
                    public @NotNull Charset getCharset() {
                        return null;
                    }

                    @Override
                    public @NotNull Charset getCharset(@Nullable Project project) {
                        return null;
                    }

                    @Override
                    public @NotNull FileType getFileType() {
                        return null;
                    }

                    @Override
                    public void refresh() {

                    }

                    @Override
                    public void hardRefresh() {

                    }

                    @Override
                    public @NlsSafe @NotNull @SystemIndependent String getPath() {
                        return "C:\\t\\test.txt";
                    }

                    @Override
                    public boolean isDirectory() {
                        return false;
                    }

                    @Override
                    public boolean isUnder(@NotNull FilePath parent, boolean strict) {
                        return false;
                    }

                    @Override
                    public @Nullable FilePath getParentPath() {
                        return null;
                    }

                    @Override
                    public boolean isNonLocal() {
                        return false;
                    }
                };
            }

            @Override
            public @NotNull VcsRevisionNumber getRevisionNumber() {
                return null;
            }
        };
        var c2 = new ContentRevision(){
            @Override
            public @Nullable String getContent() throws VcsException {
                return "Test2";
            }

            @Override
            public @NotNull FilePath getFile() {
                return new FilePath() {
                    @Override
                    public @Nullable VirtualFile getVirtualFile() {
                        return null;
                    }

                    @Override
                    public @Nullable VirtualFile getVirtualFileParent() {
                        return null;
                    }

                    @Override
                    public @NotNull File getIOFile() {
                        return new File("C:\\t\\test.txt");
                    }

                    @Override
                    public @NlsSafe @NotNull String getName() {
                        return "test.txt";
                    }

                    @Override
                    public @NlsSafe @NotNull String getPresentableUrl() {
                        return null;
                    }

                    @Override
                    public @Nullable Document getDocument() {
                        return null;
                    }

                    @Override
                    public @NotNull Charset getCharset() {
                        return null;
                    }

                    @Override
                    public @NotNull Charset getCharset(@Nullable Project project) {
                        return null;
                    }

                    @Override
                    public @NotNull FileType getFileType() {
                        return null;
                    }

                    @Override
                    public void refresh() {

                    }

                    @Override
                    public void hardRefresh() {

                    }

                    @Override
                    public @NlsSafe @NotNull @SystemIndependent String getPath() {
                        return "C:\\t\\test.txt";
                    }

                    @Override
                    public boolean isDirectory() {
                        return false;
                    }

                    @Override
                    public boolean isUnder(@NotNull FilePath parent, boolean strict) {
                        return false;
                    }

                    @Override
                    public @Nullable FilePath getParentPath() {
                        return null;
                    }

                    @Override
                    public boolean isNonLocal() {
                        return false;
                    }
                };
            }

            @Override
            public @NotNull VcsRevisionNumber getRevisionNumber() {
                return null;
            }
        };
        var change = new Change(c1,c2);
        var model = new SingleValueModel<>((Collection<Change>) List.of(change));
        var tree = new PatchProposalChangesTree(project, model).create("empty");

        var editorDiffPreview = new DiffPreview(){

            @Override
            public boolean openPreview(boolean b) {
                System.out.println("preview");
                return true;
            }

            @Override
            public void closePreview() {

            }

            @Override
            public void updatePreview(boolean b) {

            }

            @Override
            public void updateDiffAction(@NotNull AnActionEvent anActionEvent) {
                System.out.println("dsa");
            }

            @Override
            public boolean performDiffAction() {
                // File file = new File("C:\\t\\test.txt");
                // VirtualFile myF = LocalFileSystem.getInstance().findFileByIoFile(file);
                var k = new PatchProposalDiffVirtualFile();
                DiffEditorTabFilesManager.getInstance(project).showDiffFile(k,true);



                return true;
            }

        };
        tree.setDoubleClickHandler(mouseEvent -> {
            editorDiffPreview.performDiffAction();
            return true;
        });
        return ScrollPaneFactory.createScrollPane(tree,false);
    }

    public JComponent createReturnToListSideComponent(PatchTabController controller) {
        return ReturnToListComponent.INSTANCE.createReturnToListSideComponent(RadicleBundle.message("backToList"),
                () -> {controller.createPatchesPanel(); return Unit.INSTANCE;});
    }
}
