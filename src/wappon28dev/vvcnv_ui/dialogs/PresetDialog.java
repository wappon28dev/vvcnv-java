package wappon28dev.vvcnv_ui.dialogs;

import wappon28dev.vvcnv_ui.models.Preset;
import wappon28dev.vvcnv_ui.services.PresetService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * プリセット管理ダイアログ（保存・読み込み・削除）
 */
public class PresetDialog extends JDialog {

  private final PresetService presetService;
  private final Preset currentPreset;
  private final PresetSelectedCallback callback;

  private JList<String> presetList;
  private DefaultListModel<String> listModel;
  private JButton loadButton;
  private JButton deleteButton;
  private JButton saveButton;
  private JTextField presetNameField;

  public interface PresetSelectedCallback {
    void onPresetSelected(Preset preset);
  }

  public PresetDialog(Frame parent, Preset currentPreset, PresetSelectedCallback callback) {
    super(parent, "プリセット管理", true);
    this.presetService = new PresetService();
    this.currentPreset = currentPreset;
    this.callback = callback;

    initializeComponents();
    setupLayout();
    setupEventHandlers();
    loadPresetList();

    setSize(400, 300);
    setLocationRelativeTo(parent);
  }

  private void initializeComponents() {
    listModel = new DefaultListModel<>();
    presetList = new JList<>(listModel);
    presetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    loadButton = new JButton("読み込み");
    deleteButton = new JButton("削除");
    saveButton = new JButton("保存");

    presetNameField = new JTextField(20);
    presetNameField.setToolTipText("新しいプリセット名を入力");

    loadButton.setEnabled(false);
    deleteButton.setEnabled(false);
  }

  private void setupLayout() {
    setLayout(new BorderLayout());

    JScrollPane scrollPane = new JScrollPane(presetList);
    scrollPane.setBorder(BorderFactory.createTitledBorder("保存されたプリセット"));
    add(scrollPane, BorderLayout.CENTER);

    JPanel savePanel = new JPanel(new FlowLayout());
    savePanel.setBorder(BorderFactory.createTitledBorder("新しいプリセットを保存"));
    savePanel.add(new JLabel("名前:"));
    savePanel.add(presetNameField);
    savePanel.add(saveButton);
    add(savePanel, BorderLayout.NORTH);

    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.add(loadButton);
    buttonPanel.add(deleteButton);

    JButton cancelButton = new JButton("キャンセル");
    cancelButton.addActionListener(e -> dispose());
    buttonPanel.add(cancelButton);

    add(buttonPanel, BorderLayout.SOUTH);
  }

  private void setupEventHandlers() {
    presetList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        boolean hasSelection = !presetList.isSelectionEmpty();
        loadButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
      }
    });

    presetList.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2 && !presetList.isSelectionEmpty()) {
          loadSelectedPreset();
        }
      }
    });

    loadButton.addActionListener(e -> loadSelectedPreset());
    deleteButton.addActionListener(e -> deleteSelectedPreset());
    saveButton.addActionListener(e -> saveCurrentPreset());
    presetNameField.addActionListener(e -> saveCurrentPreset());
  }

  private void loadPresetList() {
    listModel.clear();
    List<Preset> presets = presetService.loadPresets();
    for (Preset preset : presets) {
      listModel.addElement(preset.name());
    }
  }

  private void loadSelectedPreset() {
    String selectedName = presetList.getSelectedValue();
    if (selectedName == null)
      return;

    Preset preset = presetService.getPreset(selectedName);
    if (preset != null) {
      callback.onPresetSelected(preset);
      dispose();
    } else {
      JOptionPane.showMessageDialog(this,
          "プリセット '" + selectedName + "' が見つかりませんでした。",
          "エラー", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void deleteSelectedPreset() {
    String selectedName = presetList.getSelectedValue();
    if (selectedName == null)
      return;

    int result = JOptionPane.showConfirmDialog(this,
        "プリセット '" + selectedName + "' を削除しますか？",
        "削除確認", JOptionPane.YES_NO_OPTION);

    if (result == JOptionPane.YES_OPTION) {
      try {
        boolean deleted = presetService.deletePreset(selectedName);
        if (deleted) {
          loadPresetList();
          JOptionPane.showMessageDialog(this,
              "プリセット '" + selectedName + "' を削除しました。",
              "削除完了", JOptionPane.INFORMATION_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(this,
              "プリセット '" + selectedName + "' が見つかりませんでした。",
              "エラー", JOptionPane.ERROR_MESSAGE);
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this,
            "プリセットの削除に失敗しました: " + e.getMessage(),
            "エラー", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void saveCurrentPreset() {
    String name = presetNameField.getText().trim();
    if (name.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "プリセット名を入力してください。",
          "エラー", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Check if preset already exists
    if (presetService.presetExists(name)) {
      int result = JOptionPane.showConfirmDialog(this,
          "プリセット '" + name + "' は既に存在します。上書きしますか？",
          "上書き確認", JOptionPane.YES_NO_OPTION);

      if (result != JOptionPane.YES_OPTION) {
        return;
      }
    }

    try {
      Preset preset = new Preset(
          name,
          currentPreset.hasAudio(),
          currentPreset.encoding(),
          currentPreset.minRes(),
          currentPreset.maxRes(),
          currentPreset.resSteps(),
          currentPreset.minCrf(),
          currentPreset.maxCrf(),
          currentPreset.crfSteps(),
          currentPreset.maxThreads());

      presetService.savePreset(preset);
      loadPresetList();
      presetNameField.setText("");

      JOptionPane.showMessageDialog(this,
          "プリセット '" + name + "' を保存しました。",
          "保存完了", JOptionPane.INFORMATION_MESSAGE);

    } catch (IOException e) {
      JOptionPane.showMessageDialog(this,
          "プリセットの保存に失敗しました: " + e.getMessage(),
          "エラー", JOptionPane.ERROR_MESSAGE);
    }
  }
}
