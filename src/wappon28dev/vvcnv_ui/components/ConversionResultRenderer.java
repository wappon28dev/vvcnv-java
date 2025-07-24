package wappon28dev.vvcnv_ui.components;

import wappon28dev.vvcnv_ui.models.ConversionResult;
import wappon28dev.vvcnv_ui.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Custom table cell renderer for conversion results
 */
public class ConversionResultRenderer extends DefaultTableCellRenderer {

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {

    return switch (value) {
      case ConversionResult result when result.success() -> createSuccessComponent(result, isSelected);
      case ConversionResult result when !result.success() -> createErrorComponent(result, isSelected);
      case String str -> createProgressComponent(str, isSelected);
      default -> super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    };
  }

  private JPanel createSuccessComponent(ConversionResult result, boolean isSelected) {
    var panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    var sizeLabel = new JLabel(result.fileSize(), JLabel.CENTER);
    sizeLabel.setFont(sizeLabel.getFont().deriveFont(Font.BOLD));
    sizeLabel.setForeground(UIUtils.getFileSizeColor(result.fileSizeMB()));

    var viewButton = new JButton("表示");
    viewButton.setPreferredSize(new Dimension(60, 25));
    viewButton.addActionListener(e -> openFile(result.outputPath()));

    panel.add(sizeLabel, BorderLayout.CENTER);
    panel.add(viewButton, BorderLayout.SOUTH);
    panel.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);

    if (isSelected) {
      panel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
    }

    return panel;
  }

  private JPanel createErrorComponent(ConversionResult result, boolean isSelected) {
    var panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    var errorLabel = new JLabel("失敗", JLabel.CENTER);
    errorLabel.setForeground(Color.RED);
    errorLabel.setToolTipText(result.error());

    panel.add(errorLabel, BorderLayout.CENTER);
    panel.setBackground(isSelected ? new Color(255, 200, 200) : new Color(255, 240, 240));

    if (isSelected) {
      panel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
    }

    return panel;
  }

  private JLabel createProgressComponent(String str, boolean isSelected) {
    var label = new JLabel(str, JLabel.CENTER);
    if (isSelected) {
      label.setOpaque(true);
      label.setBackground(Color.LIGHT_GRAY);
    }
    return label;
  }

  private void openFile(String filePath) {
    try {
      Desktop.getDesktop().open(new File(filePath));
    } catch (IOException e) {
      JOptionPane.showMessageDialog(null,
          "ファイルを開けませんでした: " + e.getMessage(),
          "エラー", JOptionPane.ERROR_MESSAGE);
    }
  }
}
