package wappon28dev.vvcnv_ui.utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public final class UIUtils {

  private UIUtils() {
    // Utility class
  }

  /**
   * Create a scrollable error dialog component
   */
  public static JScrollPane createErrorDialog(String title, String message) {
    var textArea = new JTextArea(message);
    textArea.setRows(15);
    textArea.setColumns(60);
    textArea.setEditable(false);
    textArea.setCaretPosition(0);
    textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

    return new JScrollPane(textArea);
  }

  /**
   * Format stack trace to string with length limit
   */
  public static String formatStackTrace(Exception e) {
    var trace = new StringBuilder();
    for (var element : e.getStackTrace()) {
      trace.append("  at ").append(element.toString()).append("\n");
      if (trace.length() > 1000) {
        trace.append("  ... (省略)\n");
        break;
      }
    }
    return trace.toString();
  }

  /**
   * Check if file is a video file
   */
  public static boolean isVideoFile(File file) {
    var name = file.getName().toLowerCase();
    return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") ||
        name.endsWith(".mkv") || name.endsWith(".webm") || name.endsWith(".m4v");
  }

  /**
   * Format file size in human readable format
   */
  public static String formatFileSize(long bytes) {
    if (bytes < 1024)
      return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "";
    return "%.1f %sB".formatted(bytes / Math.pow(1024, exp), pre);
  }

  /**
   * Get color based on file size (green for small, red for large)
   */
  public static Color getFileSizeColor(double fileSizeMB) {
    double ratio = Math.min(fileSizeMB / 10.0, 1.0); // Normalize to 0-1
    int red = (int) (255 * ratio);
    int green = (int) (255 * (1.0 - ratio));
    return new Color(red, green, 0);
  }
}
