package wappon28dev.vvcnv_java.modules;

import wappon28dev.vvcnv_java.util.Result;

/**
 * Video configuration - port of Rust VideoConfig
 */
public record VideoConfig(VideoRes res, int fps, int crf, boolean hasAudio) {

  /**
   * Default video configuration
   */
  public static VideoConfig defaultConfig() {
    return new VideoConfig(VideoRes.R720P, 30, 23, true);
  }

  /**
   * Generate filename suffix from configuration
   */
  public String toFileName() {
    return "--res-" + res.toFileName() + "--fps-" + fps + "--crf-" + crf;
  }

  /**
   * Check for upscaling issues
   */
  public Result<Void, String> checkUpScaling(VideoStat stat) {
    var videoStream = stat.videoStream();
    var audioStreams = stat.audioStreams();

    // Check resolution upscaling
    if (res.getWidth() > videoStream.width() || res.getHeight() > videoStream.height()) {
      return Result.err(String.format(
          "Resolution upscaling detected: %s > %dx%d",
          res.getDisplayName(),
          videoStream.width(),
          videoStream.height()));
    }

    // Check FPS upscaling
    if (fps > videoStream.fps()) {
      return Result.err(String.format(
          "FPS upscaling detected: %d > %.2f",
          fps,
          videoStream.fps()));
    }

    // Check audio requirement
    if (hasAudio && audioStreams.isEmpty()) {
      return Result.err("Audio required but not present in source video");
    }

    return Result.ok(null);
  }
}
