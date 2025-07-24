package wappon28dev.vvcnv_ui.dialogs;

import wappon28dev.vvcnv_ui.components.ConversionResultRenderer;
import wappon28dev.vvcnv_ui.models.ConversionParams;
import wappon28dev.vvcnv_ui.models.ConversionResult;
import wappon28dev.vvcnv_ui.models.ConversionTask;
import wappon28dev.vvcnv_ui.services.VideoService;
import wappon28dev.vvcnv_ui.utils.ConversionUtils;
import wappon28dev.vvcnv_ui.utils.UIUtils;
import wappon28dev.vvcnv_java.modules.*;
import wappon28dev.vvcnv_java.util.Result;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cross-test window for video conversion
 */
public class CrossTestWindow extends JDialog {

  private final JFrame parent;
  private final VideoStat videoStat;
  private final ConversionParams params;
  private final VideoService videoService;

  private JTable resultTable;
  private DefaultTableModel tableModel;
  private JProgressBar overallProgressBar;
  private JLabel statusLabel;
  private ExecutorService executorService;
  private List<ConversionTask> tasks;
  private volatile int completedTasks = 0;

  public CrossTestWindow(JFrame parent, VideoStat videoStat, ConversionParams params, VideoService videoService) {
    super(parent, "クロステスト実行", true);
    this.parent = parent;
    this.videoStat = videoStat;
    this.params = params;
    this.videoService = videoService;

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

    var columnNames = generateColumnNames();
    var rowNames = generateRowNames();

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

    var rowNames = generateRowNames();
    var rowHeaderList = new JList<>(rowNames);
    rowHeaderList.setFixedCellWidth(100);
    rowHeaderList.setFixedCellHeight(resultTable.getRowHeight());
    rowHeaderList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
        var c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setHorizontalAlignment(CENTER);
        return c;
      }
    });

    var scrollPane = new JScrollPane(resultTable);
    scrollPane.setRowHeaderView(rowHeaderList);
    scrollPane.setBorder(new TitledBorder("変換結果"));

    var progressPanel = new JPanel(new BorderLayout());
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
    var resolutions = ConversionUtils.generateResolutions(params.minRes(), params.maxRes(), params.resSteps());
    return resolutions.stream()
        .map(VideoRes::getDisplayName)
        .toArray(String[]::new);
  }

  private String[] generateRowNames() {
    var crfValues = ConversionUtils.generateCrfValues(params.minCrf(), params.maxCrf(), params.crfSteps());
    return crfValues.stream()
        .map(crf -> "CRF " + crf)
        .toArray(String[]::new);
  }

  private void generateTasks() {
    tasks = new ArrayList<>();
    var resolutions = ConversionUtils.generateResolutions(params.minRes(), params.maxRes(), params.resSteps());
    var crfValues = ConversionUtils.generateCrfValues(params.minCrf(), params.maxCrf(), params.crfSteps());

    for (int resIndex = 0; resIndex < resolutions.size(); resIndex++) {
      for (int crfIndex = 0; crfIndex < crfValues.size(); crfIndex++) {
        var res = resolutions.get(resIndex);
        var crf = crfValues.get(crfIndex);

        var config = new VideoConfig(res, 30, crf, params.hasAudio());
        var task = new ConversionTask(config, resIndex, crfIndex);
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

    if (tasks.isEmpty()) {
      statusLabel.setText("変換するタスクがありません");
      return;
    }

    System.out.println("変換タスク数: " + tasks.size());
    System.out.println("最大スレッド数: " + params.maxThreads());

    var futures = tasks.stream()
        .map(task -> CompletableFuture.runAsync(() -> processTask(task), executorService))
        .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> SwingUtilities.invokeLater(this::onAllTasksCompleted))
        .exceptionally(throwable -> {
          SwingUtilities.invokeLater(() -> {
            statusLabel.setText("変換中にエラーが発生しました: " + throwable.getMessage());
            executorService.shutdown();
          });
          return null;
        });
  }

  private void onAllTasksCompleted() {
    statusLabel.setText("全ての変換が完了しました!");
    overallProgressBar.setValue(tasks.size());

    long successCount = tasks.stream()
        .mapToLong(task -> {
          try {
            var cellValue = tableModel.getValueAt(task.crfIndex(), task.resIndex());
            return cellValue instanceof ConversionResult result && result.success() ? 1 : 0;
          } catch (Exception e) {
            return 0;
          }
        })
        .sum();

    var summary = "変換完了: %d成功 / %d全体".formatted(successCount, tasks.size());
    statusLabel.setText(summary);

    executorService.shutdown();
  }

  private void processTask(ConversionTask task) {
    System.out.printf("タスク開始: 解像度=%s, CRF=%d, 位置=(%d,%d)%n",
        task.config().res().getDisplayName(), task.config().crf(), task.resIndex(), task.crfIndex());

    try {
      SwingUtilities.invokeLater(() -> {
        tableModel.setValueAt("変換中...", task.crfIndex(), task.resIndex());
        statusLabel.setText("変換中: %s CRF%d".formatted(
            task.config().res().getDisplayName(), task.config().crf()));
      });

      var fileNameParts = FileModule.getFileName(videoStat.path());
      var outputPath = "%s/%s%s.%s".formatted(
          params.outputDir(),
          fileNameParts.name(),
          task.config().toFileName(),
          fileNameParts.extension());

      System.out.println("出力パス: " + outputPath);

      var processParams = new VideoModule.VideoProcessParams(outputPath, task.config());
      var result = videoService.getVideoModule().processSimple(videoStat, processParams);

      System.out.printf("変換結果: %s (解像度=%s, CRF=%d)%n",
          result.isOk() ? "成功" : "失敗",
          task.config().res().getDisplayName(), task.config().crf());

      SwingUtilities.invokeLater(() -> updateTaskResult(task, result, outputPath));

    } catch (Exception e) {
      System.err.println("タスク処理中にエラーが発生: " + e.getMessage());
      e.printStackTrace();

      SwingUtilities.invokeLater(() -> {
        var conversionResult = new ConversionResult(
            false, "エラー", null, "処理エラー: " + e.getMessage(), 0.0);
        tableModel.setValueAt(conversionResult, task.crfIndex(), task.resIndex());
        updateProgress();
      });
    }
  }

  private void updateTaskResult(ConversionTask task, Result<Void, String> result, String outputPath) {
    switch (result) {
      case Result.Ok<Void, String> ok -> {
        try {
          long fileSize = Files.size(Paths.get(outputPath));
          double fileSizeMB = fileSize / (1024.0 * 1024.0);
          var conversionResult = new ConversionResult(
              true, UIUtils.formatFileSize(fileSize), outputPath, null, fileSizeMB);
          tableModel.setValueAt(conversionResult, task.crfIndex(), task.resIndex());
          System.out.printf("ファイルサイズ: %.2f MB (%s)%n", fileSizeMB, UIUtils.formatFileSize(fileSize));
        } catch (IOException e) {
          System.err.println("ファイルサイズ取得エラー: " + e.getMessage());
          var conversionResult = new ConversionResult(
              false, "エラー", null, "ファイルサイズ取得失敗: " + e.getMessage(), 0.0);
          tableModel.setValueAt(conversionResult, task.crfIndex(), task.resIndex());
        }
      }
      case Result.Err<Void, String> err -> {
        System.err.println("変換エラー: " + err.error());
        var conversionResult = new ConversionResult(
            false, "失敗", null, err.error(), 0.0);
        tableModel.setValueAt(conversionResult, task.crfIndex(), task.resIndex());
      }
    }

    updateProgress();
  }

  private void updateProgress() {
    completedTasks++;
    overallProgressBar.setValue(completedTasks);
    overallProgressBar.setString("%d/%d 完了".formatted(completedTasks, tasks.size()));
  }
}
