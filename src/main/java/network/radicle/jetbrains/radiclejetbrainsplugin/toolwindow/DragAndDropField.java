package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextArea;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DragAndDropField extends JBTextArea {
    private static final Logger logger = LoggerFactory.getLogger(DragAndDropField.class);

    private final boolean enableDragAndDrop;
    private final FileService fileService;
    private final Project project;
    private List<Embed> embedList;

    public DragAndDropField(Project project, boolean allowDragAndDrop) {
        this.enableDragAndDrop = allowDragAndDrop;
        this.fileService = project.getService(FileService.class);
        this.embedList = new ArrayList<>();
        this.project = project;
        this.setLineWrap(true);
        this.setDragAndDropTarget();
    }

    public DragAndDropField(Project project) {
        this(project, true);
    }

    public void setEmbedList(List<Embed> list) {
        this.embedList = list;
    }

    public List<Embed> getEmbedList() {
        //Check if the embed is still available in the text
        return embedList.stream().filter(embed ->
                        DragAndDropField.this.getText().contains("(" + embed.getOid() + ")"))
                .collect(Collectors.toList());
    }

    private void setDragAndDropTarget() {
        if (enableDragAndDrop && !ApplicationManager.getApplication().isUnitTestMode()) {
            this.setDropTarget(new Target());
        }
    }

    private class Target extends DropTarget {
        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            Point cursorLocation = dtde.getLocation();
            int offset = DragAndDropField.this.viewToModel2D(cursorLocation);
            // Move the cursor to the calculated offset
            DragAndDropField.this.setCaretPosition(offset);
            // Set focus to the JBTextArea when dragging files over it
            DragAndDropField.this.requestFocusInWindow();
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        }

        @Override
        public void drop(DropTargetDropEvent evt) {
            try {
                evt.acceptDrop(DnDConstants.ACTION_COPY);
                var droppedFiles = (List<File>) evt
                        .getTransferable().getTransferData(
                                DataFlavor.javaFileListFlavor);
                for (var file : droppedFiles) {
                    var fileName = file.getName();
                    var fileBytes = Files.readAllBytes(file.toPath());
                    var base64 = fileService.getBase64(fileBytes);
                    var gitObjectId = fileService.calculateGitObjectId(base64);
                    if (gitObjectId == null) {
                        RadAction.showErrorNotification(project, RadicleBundle.message("uploadFileError"),
                                RadicleBundle.message("errorCalculateBase64"));
                        continue;
                    }
                    embedList.add(new Embed(gitObjectId, fileName, base64));
                    var markDown = "![" + fileName + "](" + gitObjectId + ")";
                    var dropLocation = evt.getLocation();
                    int offset = DragAndDropField.this.viewToModel2D(dropLocation);
                    if (offset >= 0 && offset <= DragAndDropField.this.getText().length()) {
                        DragAndDropField.this.getDocument().insertString(offset, markDown, null);
                    }
                }
            } catch (Exception e) {
                logger.warn("Unable to get the files from drag & drop operation", e);
            }
        }
    }
}

