package wappon28dev.vvcnv_ui.utils;

import wappon28dev.vvcnv_ui.models.Preset;

import java.util.Arrays;
import java.util.List;

/**
 * デフォルトプリセット定義
 */
public class DefaultPresets {

  public static List<Preset> getDefaultPresets() {
    return Arrays.asList(
        new Preset(
            "高品質・低圧縮",
            true,
            "H.264",
            "R720P",
            "R1080P",
            2,
            15,
            25,
            3,
            4),
        new Preset(
            "標準品質・標準圧縮",
            true,
            "H.264",
            "R480P",
            "R1080P",
            3,
            20,
            30,
            3,
            4),
        new Preset(
            "低品質・高圧縮",
            true,
            "H.264",
            "R360P",
            "R720P",
            2,
            25,
            35,
            3,
            4),
        new Preset(
            "WebM・高品質",
            true,
            "WebM",
            "R720P",
            "R1080P",
            2,
            15,
            25,
            3,
            4),
        new Preset(
            "AV1・次世代高効率",
            true,
            "AV1",
            "R720P",
            "R1080P",
            2,
            20,
            30,
            3,
            2),
        new Preset(
            "フルレンジテスト",
            true,
            "H.264",
            "R240P",
            "R2160P",
            5,
            15,
            35,
            5,
            8));
  }

  public static Preset getQuickTestPreset() {
    return new Preset(
        "クイックテスト",
        true,
        "H.264",
        "R480P",
        "R720P",
        2,
        20,
        25,
        2,
        2);
  }
}
