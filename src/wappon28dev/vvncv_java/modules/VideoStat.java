package wappon28dev.vvncv_java.modules;

import java.time.Duration;
import java.util.List;

/**
 * Video statistics - port of Rust VideoStat
 */
public record VideoStat(
    String path,
    VideoStreamInfo videoStream,
    List<AudioStreamInfo> audioStreams,
    Duration duration,
    long fileSize) {

  /**
   * Video stream information
   */
  public record VideoStreamInfo(int width, int height, double fps, String pixFmt) {
  }

  /**
   * Audio stream information
   */
  public record AudioStreamInfo(String codec, int sampleRate, int channels) {
  }
}
