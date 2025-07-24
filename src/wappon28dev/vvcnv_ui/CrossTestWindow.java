package wappon28dev.vvcnv_ui;

import wappon28dev.vvncv_java.modules.*;
import wappon28dev.vvncv_java.util.Result;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Cross-test window for video conversion
 */
public class CrossTestWindow extends JDialog {

  private final MainWindow parent;
  private final VideoStat videoStat;
  private final MainWindow.ConversionParams params;
  private final VideoModule videoModule;

  private JTable resultTable;
  private DefaultTableModel tableModel;
  private JProgressBar overallProgressBar;
  private JLabel statusLabel;
  private ExecutorService executorService;
  private List<ConversionTask> tasks;
  private volatile int completedTasks = 0;

  public CrossTestWindow(MainWindow parent, VideoStat videoStat, MainWindow.ConversionParams params) {
    super(parent, "クロステスト実行", true);
    this.parent = parent;
    this.videoStat = videoStat;
    this.params = params;

    try {
      this.videoModule = new VideoModule();
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize VideoModule", e);
    }

    initializeComponents();
    setupLayout();
    setupEventHandlers();
    generateTasks();
    startConversion();
  }

  private void initializeComponents() {
    setSize(1000, 700);
    setLocationRelativeTo(parent);
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    // Create table model
    String[] columnNames = generateColumnNames();
    String[] rowNames = generateRowNames();

    tableModel = new DefaultTableModel(rowNames.length, columnNames.length) {
      @Override
      public String getColumnName(int column) {
        return columnNames[column];
      }

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    resultTable = new JTable(tableModel);
    resultTable.setDefaultRenderer(Object.class, new ConversionResultRenderer());
    resultTable.setRowHeight(60);

    overallProgressBar = new JProgressBar(0, 100);
    overallProgressBar.setStringPainted(true);

    statusLabel = new JLabel("準備中...");
  }

  private void setupLayout() {
    setLayout(new BorderLayout());

    // Create row headers
    String[] rowNames = generateRowNames();
    JList<String> rowHeaderList = new JList<>(rowNames);
    rowHeaderList.setFixedCellWidth(100);
    rowHeaderList.setFixedCellHeight(resultTable.getRowHeight());
    rowHeaderList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setHorizontalAlignment(CENTER);
        return c;
      }
    });

    // Table panel with scroll
    JScrollPane scrollPane = new JScrollPane(resultTable);
    scrollPane.setRowHeaderView(rowHeaderList);
    scrollPane.setBorder(new TitledBorder("変換結果"));

    // Progress panel
    JPanel progressPanel = new JPanel(new BorderLayout());
    progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    progressPanel.add(statusLabel, BorderLayout.NORTH);
    progressPanel.add(overallProgressBar, BorderLayout.CENTER);

    add(scrollPane, BorderLayout.CENTER);
    add(progressPanel, BorderLayout.SOUTH);
  }

  private void setupEventHandlers() {
    addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        if (executorService != null && !executorService.isShutdown()) {
          executorService.shutdownNow();
        }
      }
    });
  }

  private String[] generateColumnNames() {
    List<VideoRes> resolutions = generateResolutions();
    return resolutions.stream()
        .map(VideoRes::getDisplayName)
        .toArray(String[]::new);
  }

  private String[] generateRowNames() {
    List<Integer> crfValues = generateCrfValues();
    return crfValues.stream()
        .map(crf -> "CRF " + crf)
        .toArray(String[]::new);
  }

  private List<VideoRes> generateResolutions() {
    List<VideoRes> allRes = VideoRes.list169();
    int minIndex = allRes.indexOf(params.minRes());
    int maxIndex = allRes.indexOf(params.maxRes());

    if (params.resSteps() == 1) {
      return List.of(params.maxRes());
    }

    List<VideoRes> result = new ArrayList<>();
    for (int i = 0; i < params.resSteps(); i++) {
      int index = minIndex + (int) Math.round((double) i * (maxIndex - minIndex) / (params.resSteps() - 1));
      result.add(allRes.get(index));
    }
    return result;
  }

  private List<Integer> generateCrfValues() {
    if (params.crfSteps() == 1) {
      return List.of(params.maxCrf());
    }

    return IntStream.range(0, params.crfSteps())
        .map(i -> params.minCrf()
            + (int) Math.round((double) i * (params.maxCrf() - params.minCrf()) / (params.crfSteps() - 1)))
        .boxed()
        .toList();
  }

  private void generateTasks() {
    tasks = new ArrayList<>();
    List<VideoRes> resolutions = generateResolutions();
    List<Integer> crfValues = generateCrfValues();

    for (int resIndex = 0; resIndex < resolutions.size(); resIndex++) {
      for (int crfIndex = 0; crfIndex < crfValues.size(); crfIndex++) {
        VideoRes res = resolutions.get(resIndex);
        int crf = crfValues.get(crfIndex);

        VideoConfig config = new VideoConfig(res, 30, crf, params.hasAudio());
        ConversionTask task = new ConversionTask(config, resIndex, crfIndex);
        tasks.add(task);
      }
    }

    overallProgressBar.setMaximum(tasks.size());
  }

  private void startConversion() {
    executorService = Executors.newFixedThreadPool(params.maxThreads());
    statusLabel.setText("変換開始...");

    // Initialize table cells
    for (int row = 0; row < tableModel.getRowCount(); row++) {
      for (int col = 0; col < tableModel.getColumnCount(); col++) {
        tableModel.setValueAt("待機中...", row, col);
      }
    }

    // Validate tasks
    if (tasks.isEmpty()) {
      statusLabel.setText("変換するタスクがありません");
      return;
    }

    System.out.println("変換タスク数: " + tasks.size());
    System.out.println("最大スレッド数: " + params.maxThreads());

    // Create futures for all tasks
    List<CompletableFuture<Void>> futures = tasks.stream()
        .map(task -> CompletableFuture.runAsync(() -> processTask(task), executorService))
        .toList();

    // Monitor completion
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> SwingUtilities.invokeLater(() -> {
          statusLabel.setText("全ての変換が完了しました!");
          overallProgressBar.setValue(tasks.size());

          // Show completion summary
          long successCount = futures.stream()
              .mapToLong(f -> {
                try {
                  f.get(); // This will complete since allOf() finished
                  return 1;
                } catch (Exception e) {
                  return 0;
                }
              })
              .sum();

          String summary = String.format("変換完了: %d成功 / %d全体", successCount, tasks.size());
          statusLabel.setText(summary);

          executorService.shutdown();
        }))
        .exceptionally(throwable -> {
          SwingUtilities.invokeLater(() -> {
            statusLabel.setText("変換中にエラーが発生しました: " + throwable.getMessage());
            executorService.shutdown();
          });
          return null;
        });
  }

  private void processTask(ConversionTask task) {
    System.out.printf("タスク開始: 解像度=%s, CRF=%d, 位置=(%d,%d)%n",
        task.config.res().getDisplayName(), task.config.crf(), task.resIndex, task.crfIndex);

    try {
      // Update UI to show task is starting
      SwingUtilities.invokeLater(() -> {
        tableModel.setValueAt("変換中...", task.crfIndex, task.resIndex);
        statusLabel.setText(String.format("変換中: %s CRF%d",
            task.config.res().getDisplayName(), task.config.crf()));
      });

      // Generate output filename
      var fileNameParts = FileModule.getFileName(videoStat.path());
      String outputPath = params.outputDir() + "/" + fileNameParts.name() +
          task.config.toFileName() + "." + fileNameParts.extension();

      System.out.println("出力パス: " + outputPath);

      // Process video
      var processParams = new VideoModule.VideoProcessParams(outputPath, task.config);
      Result<Void, String> result = videoModule.processSimple(videoStat, processParams);

      System.out.printf("変換結果: %s (解像度=%s, CRF=%d)%n",
          result.isOk() ? "成功" : "失敗",
          task.config.res().getDisplayName(), task.config.crf());

      // Update UI with result
      SwingUtilities.invokeLater(() -> {
        switch (result) {
          case Result.Ok<Void, String> ok -> {
            try {
              long fileSize = Files.size(Paths.get(outputPath));
              double fileSizeMB = fileSize / (1024.0 * 1024.0);
              ConversionResult conversionResult = new ConversionResult(
                  true, formatFileSize(fileSize), outputPath, null, fileSizeMB);
              tableModel.setValueAt(conversionResult, task.crfIndex, task.resIndex);
              System.out.printf("ファイルサイズ: %.2f MB (%s)%n", fileSizeMB, formatFileSize(fileSize));
            } catch (IOException e) {
              System.err.println("ファイルサイズ取得エラー: " + e.getMessage());
              ConversionResult conversionResult = new ConversionResult(
                  false, "エラー", null, "ファイルサイズ取得失敗: " + e.getMessage(), 0.0);
              tableModel.setValueAt(conversionResult, task.crfIndex, task.resIndex);
            }
          }
          case Result.Err<Void, String> err -> {
            System.err.println("変換エラー: " + err.error());
            ConversionResult conversionResult = new ConversionResult(
                false, "失敗", null, err.error(), 0.0);
            tableModel.setValueAt(conversionResult, task.crfIndex, task.resIndex);
          }
        }

        completedTasks++;
        overallProgressBar.setValue(completedTasks);
        overallProgressBar.setString(String.format("%d/%d 完了", completedTasks, tasks.size()));
      });

    } catch (Exception e) {
      System.err.println("タスク処理中にエラーが発生: " + e.getMessage());
      e.printStackTrace();

      SwingUtilities.invokeLater(() -> {
        ConversionResult conversionResult = new ConversionResult(
            false, "エラー", null, "処理エラー: " + e.getMessage(), 0.0);
        tableModel.setValueAt(conversionResult, task.crfIndex, task.resIndex);

        completedTasks++;
        overallProgressBar.setValue(completedTasks);
        overallProgressBar.setString(String.format("%d/%d 完了", completedTasks, tasks.size()));
      });
    }
  }

  private String formatFileSize(long bytes) {
    if (bytes < 1024)
      return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "";
    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
  }

  /**
   * Conversion task data
   */
  private record ConversionTask(VideoConfig config, int resIndex, int crfIndex) {
  }

  /**
   * Conversion result data
   */
  public record ConversionResult(boolean success, String fileSize, String outputPath, String error, double fileSizeMB) {
  }

  /**
   * Custom table cell renderer for conversion results
   */
  private class ConversionResultRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column) {

      return switch (value) {
        case ConversionResult result when result.success() -> {
          // Success case
          JPanel panel = new JPanel(new BorderLayout());
          panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

          JLabel sizeLabel = new JLabel(result.fileSize(), JLabel.CENTER);
          sizeLabel.setFont(sizeLabel.getFont().deriveFont(Font.BOLD));

          // Color based on file size (green for small, red for large)
          Color color = getFileSizeColor(result.fileSizeMB());
          sizeLabel.setForeground(color);

          JButton viewButton = new JButton("表示");
          viewButton.setPreferredSize(new Dimension(60, 25));
          viewButton.addActionListener(e -> openFile(result.outputPath()));

          panel.add(sizeLabel, BorderLayout.CENTER);
          panel.add(viewButton, BorderLayout.SOUTH);
          panel.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);

          if (isSelected) {
            panel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
          }

          yield panel;
        }
        case ConversionResult result when !result.success() -> {
          // Error case
          JPanel panel = new JPanel(new BorderLayout());
          panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

          JLabel errorLabel = new JLabel("失敗", JLabel.CENTER);
          errorLabel.setForeground(Color.RED);
          errorLabel.setToolTipText(result.error());

          panel.add(errorLabel, BorderLayout.CENTER);
          panel.setBackground(isSelected ? new Color(255, 200, 200) : new Color(255, 240, 240));

          if (isSelected) {
            panel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
          }

          yield panel;
        }
        case String str -> {
          // Progress text
          JLabel label = new JLabel(str, JLabel.CENTER);
          if (isSelected) {
            label.setOpaque(true);
            label.setBackground(Color.LIGHT_GRAY);
          }
          yield label;
        }
        default -> super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      };
    }

    /**
     * Get color based on file size (green for small, red for large)
     */
    private Color getFileSizeColor(double fileSizeMB) {
      // Assume 0-10MB range, green to red gradient
      double ratio = Math.min(fileSizeMB / 10.0, 1.0); // Normalize to 0-1

      int red = (int) (255 * ratio);
      int green = (int) (255 * (1.0 - ratio));

      return new Color(red, green, 0);
    }

    private void openFile(String filePath) {
      try {
        Desktop.getDesktop().open(new File(filePath));
      } catch (IOException e) {
        JOptionPane.showMessageDialog(CrossTestWindow.this,
            "ファイルを開けませんでした: " + e.getMessage(),
            "エラー", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
}
