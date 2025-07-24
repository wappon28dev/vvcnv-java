package wappon28dev.vvcnv_ui;

import javax.swing.*;

/**
 * Main entry point for the VVCNV GUI application
 */
public class Main {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      var mainWindow = new MainWindow();
      mainWindow.setVisible(true);
    });
  }
}
