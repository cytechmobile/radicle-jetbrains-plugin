package network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview.editor;


import com.intellij.diff.util.FileEditorBase;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview.IssueComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class IssueEditorProvider implements FileEditorProvider, DumbAware {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file instanceof IssueVirtualFile;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        var editor = new IssueEditor((IssueVirtualFile) file);
        Disposer.register(editor, Disposer.newDisposable());
        return editor;
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "RADIS";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    public static class IssueEditor extends FileEditorBase {
        private final IssueVirtualFile file;
        private JComponent panel;

        public IssueEditor(IssueVirtualFile file) {
            this.file = file;
            initPanel();
        }

        public void initPanel() {
            panel = new IssueComponent(file.getIssueModel()).create();
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
    }
}
