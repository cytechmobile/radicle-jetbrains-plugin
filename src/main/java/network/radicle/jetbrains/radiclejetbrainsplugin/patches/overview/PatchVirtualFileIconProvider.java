package network.radicle.jetbrains.radiclejetbrainsplugin.patches.overview;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import icons.CollaborationToolsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PatchVirtualFileIconProvider implements FileIconProvider {
    @Override
    public @Nullable Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        //TODO change icons based on the patch state
        if (file instanceof PatchVirtualFile) {
            return CollaborationToolsIcons.PullRequestOpen;
        }
        return null;
    }
}
