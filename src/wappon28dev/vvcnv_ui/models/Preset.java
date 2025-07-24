package wappon28dev.vvcnv_ui.models;

import org.json.JSONObject;
import wappon28dev.vvncv_java.modules.VideoRes;

/**
 * 動画変換パラメータのプリセット
 */
public record Preset(
    String name,
    boolean hasAudio,
    String encoding,
    String minRes,
    String maxRes,
    int resSteps,
    int minCrf,
    int maxCrf,
    int crfSteps,
    int maxThreads) {

  public static Preset fromConversionParams(String name, ConversionParams params) {
    return new Preset(
        name,
        params.hasAudio(),
        params.encoding(),
        params.minRes().name(),
        params.maxRes().name(),
        params.resSteps(),
        params.minCrf(),
        params.maxCrf(),
        params.crfSteps(),
        params.maxThreads());
  }

  public JSONObject toJson() {
    var json = new JSONObject();
    json.put("name", name);
    json.put("hasAudio", hasAudio);
    json.put("encoding", encoding);
    json.put("minRes", minRes);
    json.put("maxRes", maxRes);
    json.put("resSteps", resSteps);
    json.put("minCrf", minCrf);
    json.put("maxCrf", maxCrf);
    json.put("crfSteps", crfSteps);
    json.put("maxThreads", maxThreads);
    return json;
  }

  public static Preset fromJson(JSONObject json) {
    return new Preset(
        json.getString("name"),
        json.getBoolean("hasAudio"),
        json.getString("encoding"),
        json.getString("minRes"),
        json.getString("maxRes"),
        json.getInt("resSteps"),
        json.getInt("minCrf"),
        json.getInt("maxCrf"),
        json.getInt("crfSteps"),
        json.getInt("maxThreads"));
  }

  public ConversionParams toConversionParams(String inputPath, String outputDir) {
    return new ConversionParams(
        inputPath,
        outputDir,
        hasAudio,
        encoding,
        VideoRes.valueOf(minRes),
        VideoRes.valueOf(maxRes),
        resSteps,
        minCrf,
        maxCrf,
        crfSteps,
        maxThreads);
  }
}
