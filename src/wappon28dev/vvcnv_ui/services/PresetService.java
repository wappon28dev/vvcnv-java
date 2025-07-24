package wappon28dev.vvcnv_ui.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import wappon28dev.vvcnv_ui.models.Preset;
import wappon28dev.vvcnv_ui.utils.DefaultPresets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 変換プリセット管理サービス
 */
public class PresetService {

  private static final String PRESETS_DIR = System.getProperty("user.home") + "/.vvcnv";
  private static final String PRESETS_FILE = "presets.json";

  private final Path presetsPath;

  public PresetService() {
    this.presetsPath = Paths.get(PRESETS_DIR, PRESETS_FILE);
    ensurePresetsDirectoryExists();
    initializeDefaultPresetsIfNeeded();
  }

  /**
   * プリセットディレクトリが存在することを確認
   */
  private void ensurePresetsDirectoryExists() {
    try {
      Path presetsDir = Paths.get(PRESETS_DIR);
      if (!Files.exists(presetsDir)) {
        Files.createDirectories(presetsDir);
      }
    } catch (IOException e) {
      System.err.println("Failed to create presets directory: " + e.getMessage());
    }
  }

  public void savePreset(Preset preset) throws IOException {
    List<Preset> presets = loadPresets();

    presets.removeIf(p -> p.name().equals(preset.name()));
    presets.add(preset);

    savePresets(presets);
  }

  /**
   * 全プリセットの読み込み
   */
  public List<Preset> loadPresets() {
    if (!Files.exists(presetsPath)) {
      return new ArrayList<>();
    }

    try {
      String content = Files.readString(presetsPath);
      JSONObject root = new JSONObject(content);
      JSONArray presetsArray = root.getJSONArray("presets");

      List<Preset> presets = new ArrayList<>();
      for (int i = 0; i < presetsArray.length(); i++) {
        JSONObject presetJson = presetsArray.getJSONObject(i);
        presets.add(Preset.fromJson(presetJson));
      }

      return presets;
    } catch (IOException | JSONException e) {
      System.err.println("Failed to load presets: " + e.getMessage());
      return new ArrayList<>();
    }
  }

  public boolean deletePreset(String name) throws IOException {
    List<Preset> presets = loadPresets();
    boolean removed = presets.removeIf(p -> p.name().equals(name));

    if (removed) {
      savePresets(presets);
    }

    return removed;
  }

  public Preset getPreset(String name) {
    return loadPresets().stream()
        .filter(p -> p.name().equals(name))
        .findFirst()
        .orElse(null);
  }

  public boolean presetExists(String name) {
    return getPreset(name) != null;
  }

  private void savePresets(List<Preset> presets) throws IOException {
    JSONObject root = new JSONObject();
    JSONArray presetsArray = new JSONArray();

    for (Preset preset : presets) {
      presetsArray.put(preset.toJson());
    }

    root.put("presets", presetsArray);
    root.put("version", "1.0");
    root.put("created", System.currentTimeMillis());

    Files.writeString(presetsPath, root.toString(2));
  }

  public String getPresetsFilePath() {
    return presetsPath.toString();
  }

  /**
   * ファイルが存在しない場合にデフォルトプリセットを初期化
   */
  private void initializeDefaultPresetsIfNeeded() {
    if (!Files.exists(presetsPath)) {
      try {
        List<Preset> defaultPresets = DefaultPresets.getDefaultPresets();
        savePresets(defaultPresets);
        System.out.println("Default presets initialized: " + defaultPresets.size() + " presets");
      } catch (IOException e) {
        System.err.println("Failed to initialize default presets: " + e.getMessage());
      }
    }
  }
}
