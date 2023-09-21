package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor;

import com.intellij.diff.util.FileEditorBase;
import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import icons.CollaborationToolsIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.TimelineComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.JComponent;
import javax.swing.Icon;

public class PatchEditorProvider implements FileEditorProvider, DumbAware {
    private PatchEditor patchEditor;

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file instanceof PatchVirtualFile;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        var editor = new PatchEditor((PatchVirtualFile) file);
        patchEditor = editor;
        Disposer.register(editor, Disposer.newDisposable());
        return editor;
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "RADPR";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    public TimelineComponent getTimelineComponent() {
        return patchEditor.getTimelineComponent();
    }

    public static class PatchEditor extends FileEditorBase {
        private final PatchVirtualFile file;
        private JComponent panel;
        private TimelineComponent timelineComponent;

        public PatchEditor(PatchVirtualFile file) {
            this.file = file;
            initPanel();
        }

        public void initPanel() {
            timelineComponent = new TimelineComponent(file.getProposalPanel(), file);
            panel = timelineComponent.create();
            panel.setOpaque(true);
            panel.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
        }

        @Override
        public @NotNull JComponent getComponent() {
            return panel;
        }

        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return null;
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
            return "";
        }

        @Override
        public VirtualFile getFile() {
            return file;
        }

        public TimelineComponent getTimelineComponent() {
            return timelineComponent;
        }
    }

    public static class PatchVirtualFileIconProvider implements FileIconProvider {
        @Override
        public @Nullable Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
            //TODO change icon based on the patch state
            if (file instanceof PatchVirtualFile) {
                return CollaborationToolsIcons.PullRequestOpen;
            }
            return null;
        }
    }

}
