package wappon28dev.vvcnv_ui.components;

import wappon28dev.vvcnv_ui.utils.UIUtils;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Drag and drop handler for file inputs
 */
public class FileDropHandler implements DropTargetListener {

  private final JTextField targetField;
  private final Consumer<File> onValidFileDrop;
  private final boolean acceptDirectories;

  public FileDropHandler(JTextField targetField, Consumer<File> onValidFileDrop, boolean acceptDirectories) {
    this.targetField = targetField;
    this.onValidFileDrop = onValidFileDrop;
    this.acceptDirectories = acceptDirectories;

    new DropTarget(targetField, this);
  }

  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      dtde.acceptDrag(DnDConstants.ACTION_COPY);
    } else {
      dtde.rejectDrag();
    }
  }

  @Override
  public void dragOver(DropTargetDragEvent dtde) {
    // No implementation needed
  }

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {
    // No implementation needed
  }

  @Override
  public void dragExit(DropTargetEvent dte) {
    // No implementation needed
  }

  @Override
  public void drop(DropTargetDropEvent dtde) {
    try {
      dtde.acceptDrop(DnDConstants.ACTION_COPY);
      Transferable transferable = dtde.getTransferable();

      if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        @SuppressWarnings("unchecked")
        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

        if (!files.isEmpty()) {
          File file = files.getFirst();

          if (acceptDirectories && file.isDirectory()) {
            targetField.setText(file.getAbsolutePath());
            onValidFileDrop.accept(file);
          } else if (!acceptDirectories && UIUtils.isVideoFile(file)) {
            targetField.setText(file.getAbsolutePath());
            onValidFileDrop.accept(file);
          } else {
            String errorMessage = acceptDirectories ? "フォルダを選択してください。" : "サポートされていないファイル形式です。";
            JOptionPane.showMessageDialog(null, errorMessage, "エラー", JOptionPane.ERROR_MESSAGE);
          }
        }
      }
      dtde.dropComplete(true);
    } catch (Exception e) {
      e.printStackTrace();
      dtde.dropComplete(false);
    }
  }
}
