package wappon28dev.vvcnv_ui;

import wappon28dev.vvncv_java.modules.*;
import wappon28dev.vvncv_java.util.Result;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main window for the VVCNV GUI application
 */
public class MainWindow extends JFrame {

  private static final String DEFAULT_OUTPUT_DIR = "out";

  // UI Components
  private JTextField inputFileField;
  private JTextField outputDirField;
  private JCheckBox audioCheckBox;
  private JComboBox<String> encodingComboBox;
  private JComboBox<VideoRes> minResComboBox;
  private JComboBox<VideoRes> maxResComboBox;
  private JSpinner resStepsSpinner;
  private JSpinner minCrfSpinner;
  private JSpinner maxCrfSpinner;
  private JSpinner crfStepsSpinner;
  private JSpinner maxThreadsSpinner;
  private JButton startButton;
  private JButton selectInputButton;
  private JButton selectOutputButton;

  // Data
  private VideoModule videoModule;
  private VideoStat currentVideoStat;

  public MainWindow() {
    initializeComponents();
    setupLayout();
    setupEventHandlers();
    initializeVideoModule();
  }

  private void initializeComponents() {
    setTitle("VVCNV - Video Variant Converter");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(800, 600);
    setLocationRelativeTo(null);

    // Input/Output components
    inputFileField = new JTextField();
    inputFileField.setEditable(false);
    outputDirField = new JTextField(DEFAULT_OUTPUT_DIR);

    selectInputButton = new JButton("ファイル選択");
    selectOutputButton = new JButton("フォルダ選択");

    // Video configuration components
    audioCheckBox = new JCheckBox("音声を保持", true);
    encodingComboBox = new JComboBox<>(new String[] { "H.264", "WebM", "AV1" });

    // Resolution configuration
    var resolutions = VideoRes.list169().toArray(new VideoRes[0]);
    minResComboBox = new JComboBox<>(resolutions);
    maxResComboBox = new JComboBox<>(resolutions);
    minResComboBox.setSelectedItem(VideoRes.R240P);
    maxResComboBox.setSelectedItem(VideoRes.R1080P);
    resStepsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));

    // CRF configuration
    minCrfSpinner = new JSpinner(new SpinnerNumberModel(15, 0, 51, 1));
    maxCrfSpinner = new JSpinner(new SpinnerNumberModel(35, 0, 51, 1));
    crfStepsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));

    // Other settings
    maxThreadsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));

    startButton = new JButton("変換開始");
    startButton.setFont(startButton.getFont().deriveFont(Font.BOLD, 16f));
    startButton.setPreferredSize(new Dimension(200, 40));

    // Enable drag and drop
    setupDragAndDrop();
  }

  private void setupLayout() {
    setLayout(new BorderLayout());

    // Main panel with padding
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    // Input/Output panel
    JPanel ioPanel = createIOPanel();

    // Configuration panel
    JPanel configPanel = createConfigurationPanel();

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.add(startButton);

    // Combine panels
    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(ioPanel, BorderLayout.NORTH);
    topPanel.add(configPanel, BorderLayout.CENTER);

    mainPanel.add(topPanel, BorderLayout.CENTER);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel, BorderLayout.CENTER);
  }

  private JPanel createIOPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(new TitledBorder("入力・出力設定"));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;

    // Input file
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(new JLabel("入力動画:"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(inputFileField, gbc);

    gbc.gridx = 2;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(selectInputButton, gbc);

    // Output directory
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 0;
    panel.add(new JLabel("出力先:"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(outputDirField, gbc);

    gbc.gridx = 2;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(selectOutputButton, gbc);

    return panel;
  }

  private JPanel createConfigurationPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(new TitledBorder("書き出しパラメーター"));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;

    int row = 0;

    // Audio and encoding
    gbc.gridx = 0;
    gbc.gridy = row;
    panel.add(audioCheckBox, gbc);

    gbc.gridx = 1;
    panel.add(new JLabel("エンコーディング:"), gbc);

    gbc.gridx = 2;
    panel.add(encodingComboBox, gbc);

    row++;

    // Resolution settings
    gbc.gridx = 0;
    gbc.gridy = row;
    panel.add(new JLabel("解像度範囲:"), gbc);

    gbc.gridx = 1;
    panel.add(minResComboBox, gbc);

    gbc.gridx = 2;
    panel.add(new JLabel("〜"), gbc);

    gbc.gridx = 3;
    panel.add(maxResComboBox, gbc);

    gbc.gridx = 4;
    panel.add(new JLabel("ステップ数:"), gbc);

    gbc.gridx = 5;
    panel.add(resStepsSpinner, gbc);

    row++;

    // CRF settings
    gbc.gridx = 0;
    gbc.gridy = row;
    panel.add(new JLabel("品質レベル(CRF):"), gbc);

    gbc.gridx = 1;
    panel.add(minCrfSpinner, gbc);

    gbc.gridx = 2;
    panel.add(new JLabel("〜"), gbc);

    gbc.gridx = 3;
    panel.add(maxCrfSpinner, gbc);

    gbc.gridx = 4;
    panel.add(new JLabel("ステップ数:"), gbc);

    gbc.gridx = 5;
    panel.add(crfStepsSpinner, gbc);

    row++;

    // Thread settings
    gbc.gridx = 0;
    gbc.gridy = row;
    panel.add(new JLabel("最大並列実行数:"), gbc);

    gbc.gridx = 1;
    panel.add(maxThreadsSpinner, gbc);

    return panel;
  }

  private void setupDragAndDrop() {
    // Enable drag and drop for input file field
    new DropTarget(inputFileField, new DropTargetListener() {
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
      }

      @Override
      public void dropActionChanged(DropTargetDragEvent dtde) {
      }

      @Override
      public void dragExit(DropTargetEvent dte) {
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
              File file = files.get(0);
              if (isVideoFile(file)) {
                inputFileField.setText(file.getAbsolutePath());
                loadVideoStats(file.getAbsolutePath());
              } else {
                JOptionPane.showMessageDialog(MainWindow.this,
                    "サポートされていないファイル形式です。", "エラー", JOptionPane.ERROR_MESSAGE);
              }
            }
          }
          dtde.dropComplete(true);
        } catch (Exception e) {
          e.printStackTrace();
          dtde.dropComplete(false);
        }
      }
    });

    // Enable drag and drop for output directory field
    new DropTarget(outputDirField, new DropTargetListener() {
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
      }

      @Override
      public void dropActionChanged(DropTargetDragEvent dtde) {
      }

      @Override
      public void dragExit(DropTargetEvent dte) {
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
              File file = files.get(0);
              if (file.isDirectory()) {
                outputDirField.setText(file.getAbsolutePath());
              } else {
                JOptionPane.showMessageDialog(MainWindow.this,
                    "フォルダを選択してください。", "エラー", JOptionPane.ERROR_MESSAGE);
              }
            }
          }
          dtde.dropComplete(true);
        } catch (Exception e) {
          e.printStackTrace();
          dtde.dropComplete(false);
        }
      }
    });
  }

  private void setupEventHandlers() {
    selectInputButton.addActionListener(e -> selectInputFile());
    selectOutputButton.addActionListener(e -> selectOutputDirectory());
    startButton.addActionListener(e -> startConversion());

    // Update audio checkbox based on video stats
    inputFileField.addPropertyChangeListener("text", e -> updateAudioCheckbox());
  }

  private void initializeVideoModule() {
    try {
      System.out.println("VideoModuleを初期化中...");
      videoModule = new VideoModule();
      System.out.println("VideoModule初期化完了");
    } catch (IOException e) {
      System.err.println("VideoModule初期化エラー:");
      e.printStackTrace();

      var errorDialog = createErrorDialog(
          "FFmpeg初期化エラー",
          """
              FFmpegの初期化に失敗しました:

              エラー: %s

              考えられる原因:
              - FFmpegがインストールされていない
              - FFmpegのパスが正しくない
              - FFmpegの実行権限がない

              詳細なスタックトレース:
              %s
              """.formatted(e.getMessage(), formatStackTrace(e)));

      JOptionPane.showMessageDialog(this, errorDialog,
          "FFmpeg初期化エラー", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }

  private void selectInputFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter(
        "動画ファイル", "mp4", "avi", "mov", "mkv", "webm", "m4v"));

    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      inputFileField.setText(selectedFile.getAbsolutePath());
      loadVideoStats(selectedFile.getAbsolutePath());
    }
  }

  private void selectOutputDirectory() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedDir = fileChooser.getSelectedFile();
      outputDirField.setText(selectedDir.getAbsolutePath());
    }
  }

  private void loadVideoStats(String inputPath) {
    SwingWorker<VideoStat, Void> worker = new SwingWorker<>() {
      @Override
      protected VideoStat doInBackground() throws Exception {
        try {
          System.out.println("動画統計情報を読み込み中: " + inputPath);
          Result<VideoStat, String> result = videoModule.stat(inputPath);

          return switch (result) {
            case Result.Ok<VideoStat, String> ok -> {
              System.out.println("動画統計情報の読み込み完了");
              yield ok.value();
            }
            case Result.Err<VideoStat, String> err -> {
              System.err.println("VideoModule.stat() エラー: " + err.error());
              throw new RuntimeException("VideoModule.stat() failed: " + err.error());
            }
          };
        } catch (Exception e) {
          System.err.println("doInBackground()でエラーが発生:");
          e.printStackTrace();
          throw e; // Re-throw to be caught in done()
        }
      }

      @Override
      protected void done() {
        try {
          currentVideoStat = get();
          updateAudioCheckbox();
          updateResolutionLimits();

          var videoInfo = """
              動画情報を読み込みました:
              %dx%d @ %.2ffps
              時間: %.2f秒
              """.formatted(
              currentVideoStat.videoStream().width(),
              currentVideoStat.videoStream().height(),
              currentVideoStat.videoStream().fps(),
              (double) currentVideoStat.duration().toSeconds());

          System.out.println(videoInfo);
        } catch (Exception e) {
          System.err.println("動画情報の読み込みに失敗しました:");
          e.printStackTrace();

          var errorDialog = createErrorDialog(
              "エラー詳細",
              """
                  動画情報の読み込みに失敗しました:

                  エラー: %s

                  詳細:
                  %s
                  """.formatted(e.getMessage(), formatStackTrace(e)));

          JOptionPane.showMessageDialog(MainWindow.this, errorDialog,
              "エラー詳細", JOptionPane.ERROR_MESSAGE);
        }
      }
    };
    worker.execute();
  }

  /**
   * Create a scrollable error dialog component
   */
  private JScrollPane createErrorDialog(String title, String message) {
    JTextArea textArea = new JTextArea(message);
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
  private String formatStackTrace(Exception e) {
    StringBuilder trace = new StringBuilder();
    for (StackTraceElement element : e.getStackTrace()) {
      trace.append("  at ").append(element.toString()).append("\n");
      if (trace.length() > 1000) {
        trace.append("  ... (省略)\n");
        break;
      }
    }
    return trace.toString();
  }

  private void updateAudioCheckbox() {
    if (currentVideoStat != null) {
      boolean hasAudio = !currentVideoStat.audioStreams().isEmpty();
      audioCheckBox.setEnabled(hasAudio);
      if (!hasAudio) {
        audioCheckBox.setSelected(false);
        audioCheckBox.setText("音声を保持 (音声トラックなし)");
      } else {
        audioCheckBox.setText("音声を保持");
      }
    }
  }

  private void updateResolutionLimits() {
    if (currentVideoStat != null) {
      var videoStream = currentVideoStat.videoStream();
      var resolutions = VideoRes.list169();

      // Filter resolutions that would cause upscaling
      var validResolutions = resolutions.stream()
          .filter(res -> res.getWidth() <= videoStream.width() && res.getHeight() <= videoStream.height())
          .toList();

      if (!validResolutions.isEmpty()) {
        minResComboBox.setModel(new DefaultComboBoxModel<>(validResolutions.toArray(new VideoRes[0])));
        maxResComboBox.setModel(new DefaultComboBoxModel<>(validResolutions.toArray(new VideoRes[0])));

        minResComboBox.setSelectedIndex(0);
        maxResComboBox.setSelectedIndex(validResolutions.size() - 1);
      }
    }
  }

  private void startConversion() {
    if (inputFileField.getText().isEmpty()) {
      JOptionPane.showMessageDialog(this, "入力ファイルを選択してください。", "エラー", JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (currentVideoStat == null) {
      JOptionPane.showMessageDialog(this, "動画情報を読み込んでください。", "エラー", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Create output directory
    try {
      Files.createDirectories(Paths.get(outputDirField.getText()));
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "出力ディレクトリの作成に失敗しました: " + e.getMessage(),
          "エラー", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Show cross-test window
    showCrossTestWindow();
  }

  private void showCrossTestWindow() {
    var crossTestWindow = new CrossTestWindow(this, currentVideoStat, createConversionParams());
    crossTestWindow.setVisible(true);
  }

  private ConversionParams createConversionParams() {
    return new ConversionParams(
        inputFileField.getText(),
        outputDirField.getText(),
        audioCheckBox.isSelected(),
        (String) encodingComboBox.getSelectedItem(),
        (VideoRes) minResComboBox.getSelectedItem(),
        (VideoRes) maxResComboBox.getSelectedItem(),
        (Integer) resStepsSpinner.getValue(),
        (Integer) minCrfSpinner.getValue(),
        (Integer) maxCrfSpinner.getValue(),
        (Integer) crfStepsSpinner.getValue(),
        (Integer) maxThreadsSpinner.getValue());
  }

  private boolean isVideoFile(File file) {
    String name = file.getName().toLowerCase();
    return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") ||
        name.endsWith(".mkv") || name.endsWith(".webm") || name.endsWith(".m4v");
  }

  /**
   * Parameters for video conversion
   */
  public record ConversionParams(
      String inputPath,
      String outputDir,
      boolean hasAudio,
      String encoding,
      VideoRes minRes,
      VideoRes maxRes,
      int resSteps,
      int minCrf,
      int maxCrf,
      int crfSteps,
      int maxThreads) {
  }
}
