package network.radicle.jetbrains.radiclejetbrainsplugin.patches.overview;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PatchEditorProvider implements FileEditorProvider, DumbAware {
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file instanceof PatchVirtualFile;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new PatchTimeLineEditor((PatchVirtualFile) file);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "RADPR";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
