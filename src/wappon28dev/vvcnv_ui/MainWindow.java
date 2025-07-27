package wappon28dev.vvcnv_ui;

import wappon28dev.vvcnv_ui.components.FileDropHandler;
import wappon28dev.vvcnv_ui.dialogs.CrossTestWindow;
import wappon28dev.vvcnv_ui.dialogs.PresetDialog;
import wappon28dev.vvcnv_ui.models.ConversionParams;
import wappon28dev.vvcnv_ui.models.Preset;
import wappon28dev.vvcnv_ui.services.VideoService;
import wappon28dev.vvcnv_ui.utils.ConversionUtils;
import wappon28dev.vvcnv_ui.utils.UIUtils;
import wappon28dev.vvcnv_java.modules.VideoRes;
import wappon28dev.vvcnv_java.modules.VideoStat;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main window for the VVCNV GUI application
 */
public class MainWindow extends JFrame {

  private static final String DEFAULT_OUTPUT_DIR = "output";

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
  private JButton savePresetButton;
  private JButton loadPresetButton;

  // Services
  private VideoService videoService;
  private VideoStat currentVideoStat;

  public MainWindow() {
    initializeServices();
    initializeComponents();
    setupLayout();
    setupEventHandlers();
    setupDragAndDrop();
  }

  private void initializeServices() {
    try {
      System.out.println("VideoServiceを初期化中...");
      videoService = new VideoService();
      System.out.println("VideoService初期化完了");
    } catch (IOException e) {
      handleVideoServiceInitError(e);
    }
  }

  private void handleVideoServiceInitError(IOException e) {
    System.err.println("VideoService初期化エラー:");
    e.printStackTrace();

    var errorDialog = UIUtils.createErrorDialog(
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
            """.formatted(e.getMessage(), UIUtils.formatStackTrace(e)));

    JOptionPane.showMessageDialog(this, errorDialog,
        "FFmpeg初期化エラー", JOptionPane.ERROR_MESSAGE);
    System.exit(1);
  }

  private void initializeComponents() {
    setTitle("vvcnv");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(800, 600);
    setLocationRelativeTo(null);

    createIOComponents();
    createConfigComponents();
    createActionComponents();
  }

  private void createIOComponents() {
    inputFileField = new JTextField();
    inputFileField.setEditable(false);
    outputDirField = new JTextField(DEFAULT_OUTPUT_DIR);

    selectInputButton = new JButton("ファイル選択");
    selectOutputButton = new JButton("フォルダ選択");
  }

  private void createConfigComponents() {
    audioCheckBox = new JCheckBox("音声を保持", true);
    encodingComboBox = new JComboBox<>(new String[] { "H.264", "WebM", "AV1" });

    var resolutions = VideoRes.list169().toArray(new VideoRes[0]);
    minResComboBox = new JComboBox<>(resolutions);
    maxResComboBox = new JComboBox<>(resolutions);
    minResComboBox.setSelectedItem(VideoRes.R240P);
    maxResComboBox.setSelectedItem(VideoRes.R1080P);

    resStepsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
    minCrfSpinner = new JSpinner(new SpinnerNumberModel(15, 0, 51, 1));
    maxCrfSpinner = new JSpinner(new SpinnerNumberModel(35, 0, 51, 1));
    crfStepsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
    maxThreadsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
  }

  private void createActionComponents() {
    startButton = new JButton("変換開始");
    startButton.setFont(startButton.getFont().deriveFont(Font.BOLD, 16f));
    startButton.setPreferredSize(new Dimension(200, 40));

    savePresetButton = new JButton("プリセット保存");
    loadPresetButton = new JButton("プリセット読込");
  }

  private void setupLayout() {
    setLayout(new BorderLayout());

    var mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    var topPanel = new JPanel(new BorderLayout());
    topPanel.add(createIOPanel(), BorderLayout.NORTH);
    topPanel.add(createConfigurationPanel(), BorderLayout.CENTER);

    var buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.add(loadPresetButton);
    buttonPanel.add(savePresetButton);
    buttonPanel.add(startButton);

    mainPanel.add(topPanel, BorderLayout.CENTER);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel, BorderLayout.CENTER);
  }

  private JPanel createIOPanel() {
    var panel = new JPanel(new GridBagLayout());
    panel.setBorder(new TitledBorder("入力・出力設定"));

    var gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;

    addIORow(panel, gbc, 0, "入力動画:", inputFileField, selectInputButton);
    addIORow(panel, gbc, 1, "出力先:", outputDirField, selectOutputButton);

    return panel;
  }

  private void addIORow(JPanel panel, GridBagConstraints gbc, int row,
      String label, JTextField field, JButton button) {
    gbc.gridy = row;

    gbc.gridx = 0;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(new JLabel(label), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(field, gbc);

    gbc.gridx = 2;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(button, gbc);
  }

  private JPanel createConfigurationPanel() {
    var panel = new JPanel(new GridBagLayout());
    panel.setBorder(new TitledBorder("書き出しパラメーター"));

    var gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;

    addConfigRow1(panel, gbc);
    addConfigRow2(panel, gbc);
    addConfigRow3(panel, gbc);
    addConfigRow4(panel, gbc);

    return panel;
  }

  private void addConfigRow1(JPanel panel, GridBagConstraints gbc) {
    gbc.gridy = 0;
    gbc.gridx = 0;
    panel.add(audioCheckBox, gbc);
    gbc.gridx = 1;
    panel.add(new JLabel("エンコーディング:"), gbc);
    gbc.gridx = 2;
    panel.add(encodingComboBox, gbc);
  }

  private void addConfigRow2(JPanel panel, GridBagConstraints gbc) {
    gbc.gridy = 1;
    gbc.gridx = 0;
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
  }

  private void addConfigRow3(JPanel panel, GridBagConstraints gbc) {
    gbc.gridy = 2;
    gbc.gridx = 0;
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
  }

  private void addConfigRow4(JPanel panel, GridBagConstraints gbc) {
    gbc.gridy = 3;
    gbc.gridx = 0;
    panel.add(new JLabel("最大並列実行数:"), gbc);
    gbc.gridx = 1;
    panel.add(maxThreadsSpinner, gbc);
  }

  private void setupDragAndDrop() {
    new FileDropHandler(inputFileField, this::onInputFileDrop, false);
    new FileDropHandler(outputDirField, this::onOutputDirDrop, true);
  }

  private void onInputFileDrop(File file) {
    // Auto-generate output directory path
    String defaultOutputDir = ConversionUtils.generateDefaultOutputDir(file.getAbsolutePath());
    outputDirField.setText(defaultOutputDir);

    loadVideoStats(file.getAbsolutePath());
  }

  private void onOutputDirDrop(File file) {
    // File drop handler already set the text field
  }

  private void setupEventHandlers() {
    selectInputButton.addActionListener(e -> selectInputFile());
    selectOutputButton.addActionListener(e -> selectOutputDirectory());
    startButton.addActionListener(e -> startConversion());
    savePresetButton.addActionListener(e -> showSavePresetDialog());
    loadPresetButton.addActionListener(e -> showLoadPresetDialog());
    inputFileField.addPropertyChangeListener("text", e -> updateAudioCheckbox());
  }

  private void selectInputFile() {
    var fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter(
        "動画ファイル", "mp4", "avi", "mov", "mkv", "webm", "m4v"));

    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      var selectedFile = fileChooser.getSelectedFile();
      inputFileField.setText(selectedFile.getAbsolutePath());

      // Auto-generate output directory path
      String defaultOutputDir = ConversionUtils.generateDefaultOutputDir(selectedFile.getAbsolutePath());
      outputDirField.setText(defaultOutputDir);

      loadVideoStats(selectedFile.getAbsolutePath());
    }
  }

  private void selectOutputDirectory() {
    var fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      var selectedDir = fileChooser.getSelectedFile();
      outputDirField.setText(selectedDir.getAbsolutePath());
    }
  }

  private void loadVideoStats(String inputPath) {
    var worker = videoService.loadVideoStatsAsync(inputPath);
    worker.addPropertyChangeListener(evt -> {
      if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE == evt.getNewValue()) {
        handleVideoStatsLoaded(worker);
      }
    });
    worker.execute();
  }

  private void handleVideoStatsLoaded(SwingWorker<VideoStat, Void> worker) {
    try {
      currentVideoStat = worker.get();
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
      handleVideoStatsError(e);
    }
  }

  private void handleVideoStatsError(Exception e) {
    System.err.println("動画情報の読み込みに失敗しました:");
    e.printStackTrace();

    var errorDialog = UIUtils.createErrorDialog(
        "エラー詳細",
        """
            動画情報の読み込みに失敗しました:

            エラー: %s

            詳細:
            %s
            """.formatted(e.getMessage(), UIUtils.formatStackTrace(e)));

    JOptionPane.showMessageDialog(this, errorDialog,
        "エラー詳細", JOptionPane.ERROR_MESSAGE);
  }

  private void updateAudioCheckbox() {
    if (currentVideoStat != null) {
      boolean hasAudio = !currentVideoStat.audioStreams().isEmpty();
      audioCheckBox.setEnabled(hasAudio);
      audioCheckBox.setText(hasAudio ? "音声を保持" : "音声を保持 (音声トラックなし)");
      if (!hasAudio) {
        audioCheckBox.setSelected(false);
      }
    }
  }

  private void updateResolutionLimits() {
    if (currentVideoStat == null)
      return;

    var videoStream = currentVideoStat.videoStream();
    var validResolutions = VideoRes.list169().stream()
        .filter(res -> res.getWidth() <= videoStream.width() && res.getHeight() <= videoStream.height())
        .toList();

    if (!validResolutions.isEmpty()) {
      updateResolutionComboBoxes(validResolutions);
    }
  }

  private void updateResolutionComboBoxes(java.util.List<VideoRes> validResolutions) {
    var resolutionArray = validResolutions.toArray(new VideoRes[0]);
    minResComboBox.setModel(new DefaultComboBoxModel<>(resolutionArray));
    maxResComboBox.setModel(new DefaultComboBoxModel<>(resolutionArray));

    minResComboBox.setSelectedIndex(0);
    maxResComboBox.setSelectedIndex(validResolutions.size() - 1);
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

    try {
      Files.createDirectories(Paths.get(outputDirField.getText()));
      showCrossTestWindow();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "出力ディレクトリの作成に失敗しました: " + e.getMessage(),
          "エラー", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void showCrossTestWindow() {
    var crossTestWindow = new CrossTestWindow(this, currentVideoStat, createConversionParams(), videoService);
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

  /**
   * Show preset save/load dialog
   */
  private void showSavePresetDialog() {
    var currentPreset = createCurrentPreset();
    var dialog = new PresetDialog(this, currentPreset, this::applyPreset);
    dialog.setVisible(true);
  }

  /**
   * Show preset load dialog
   */
  private void showLoadPresetDialog() {
    var currentPreset = createCurrentPreset();
    var dialog = new PresetDialog(this, currentPreset, this::applyPreset);
    dialog.setVisible(true);
  }

  /**
   * Create preset from current UI state
   */
  private Preset createCurrentPreset() {
    return new Preset(
        "", // Name will be set by user in dialog
        audioCheckBox.isSelected(),
        (String) encodingComboBox.getSelectedItem(),
        ((VideoRes) minResComboBox.getSelectedItem()).name(),
        ((VideoRes) maxResComboBox.getSelectedItem()).name(),
        (Integer) resStepsSpinner.getValue(),
        (Integer) minCrfSpinner.getValue(),
        (Integer) maxCrfSpinner.getValue(),
        (Integer) crfStepsSpinner.getValue(),
        (Integer) maxThreadsSpinner.getValue());
  }

  /**
   * Apply preset to UI components
   */
  private void applyPreset(Preset preset) {
    try {
      audioCheckBox.setSelected(preset.hasAudio());
      encodingComboBox.setSelectedItem(preset.encoding());

      // Set resolution combo boxes
      VideoRes minRes = VideoRes.valueOf(preset.minRes());
      VideoRes maxRes = VideoRes.valueOf(preset.maxRes());
      minResComboBox.setSelectedItem(minRes);
      maxResComboBox.setSelectedItem(maxRes);

      resStepsSpinner.setValue(preset.resSteps());
      minCrfSpinner.setValue(preset.minCrf());
      maxCrfSpinner.setValue(preset.maxCrf());
      crfStepsSpinner.setValue(preset.crfSteps());
      maxThreadsSpinner.setValue(preset.maxThreads());

      JOptionPane.showMessageDialog(this,
          "プリセット '" + preset.name() + "' を適用しました。",
          "プリセット適用", JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "プリセットの適用に失敗しました: " + e.getMessage(),
          "エラー", JOptionPane.ERROR_MESSAGE);
    }
  }
}
