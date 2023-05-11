package network.radicle.jetbrains.radiclejetbrainsplugin.patches.overview;

import com.intellij.diff.util.FileEditorBase;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PatchTimeLineEditor extends FileEditorBase {
    private final PatchVirtualFile file;

    public PatchTimeLineEditor(PatchVirtualFile file) {
        this.file = file;
    }

    @Override
    public @NotNull JComponent getComponent() {
        JComponent panel = null;
        try {
            panel = new PatchEditorComponent(file.getPatch()).create();
        } catch (VcsException e) {
            throw new RuntimeException(e);
        }
        panel.setOpaque(true);
        panel.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
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
}
