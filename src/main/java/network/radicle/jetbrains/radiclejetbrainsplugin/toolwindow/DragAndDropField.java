package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.dsl.builder.DslComponentProperty;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.FileService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.awt.Rectangle;
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

public class DragAndDropField extends EditorTextField {
    private static final Logger logger = LoggerFactory.getLogger(DragAndDropField.class);
    private int border = 0;
    private final boolean enableDragAndDrop;
    private final FileService fileService;
    private final Project project;
    private List<Embed> embedList;

    public DragAndDropField(Project project, boolean allowDragAndDrop) {
        this.enableDragAndDrop = allowDragAndDrop;
        this.fileService = project.getService(FileService.class);
        this.embedList = new ArrayList<>();
        this.project = project;
        this.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
        this.addDocumentListener(new MyListener(this));
    }

    public DragAndDropField(Project project, int border, boolean allowDragAndDrop) {
        this(project, allowDragAndDrop);
        this.border = border;
    }

    @Override
    protected @NotNull EditorEx createEditor() {
        var editor =  super.createEditor();
        editor.getSettings().setUseSoftWraps(true);
        editor.setBorder(JBUI.Borders.empty(border));
        editor.setOneLineMode(false);
        editor.setVerticalScrollbarVisible(true);
        editor.getComponent().setOpaque(false);
        editor.getScrollPane().setOpaque(false);
        return editor;
    }

     @Override
     protected void onEditorAdded(@NotNull Editor editor) {
         this.putClientProperty(DslComponentProperty.INTERACTIVE_COMPONENT, editor);
         if (!enableDragAndDrop || ApplicationManager.getApplication().isUnitTestMode()) {
             return;
         }
         editor.getContentComponent().setDropTarget(new Target());
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

    public static class MyListener implements DocumentListener {
        private final EditorTextField myField;

        public MyListener(EditorTextField myField) {
            this.myField = myField;
        }

        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            var parent = myField.getParent();
            if (parent != null) {
                ((JComponent) parent).scrollRectToVisible(new Rectangle(0, 0,
                        parent.getWidth(), parent.getHeight()));
            }
        }
    }

    private class Target extends DropTarget {
        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            Point cursorLocation = dtde.getLocation();
            var logicalPosition = DragAndDropField.this.getEditor().xyToLogicalPosition(cursorLocation);
            int offset = DragAndDropField.this.getEditor().logicalPositionToOffset(logicalPosition);
            DragAndDropField.this.setCaretPosition(offset);
            DragAndDropField.this.requestFocusInWindow();
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        }

        @Override
        public void drop(DropTargetDropEvent evt) {
            try {
                evt.acceptDrop(DnDConstants.ACTION_COPY);
                var droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
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
                    var logicalPosition = DragAndDropField.this.getEditor().xyToLogicalPosition(dropLocation);
                    int offset = DragAndDropField.this.getEditor().logicalPositionToOffset(logicalPosition);
                    if (offset >= 0 && offset <= DragAndDropField.this.getText().length()) {
                        WriteCommandAction.runWriteCommandAction(project, () ->
                                DragAndDropField.this.getEditor().getDocument().insertString(offset, markDown));
                    }
                }
            } catch (Exception e) {
                logger.warn("Unable to get the files from drag & drop operation", e);
            }
        }
    }
}



