package wappon28dev.vvcnv_java.modules;

import wappon28dev.vvcnv_java.util.Result;
import java.util.List;

/**
 * Video resolution enum - port of Rust VideoRes
 */
public enum VideoRes {
  R240P(426, 240, "240p (SD)"),
  R360P(640, 360, "360p (SD)"),
  R480P(854, 480, "480p (SD)"),
  R720P(1280, 720, "720p (HD)"),
  R1080P(1920, 1080, "1080p (FHD)"),
  R1440P(2560, 1440, "1440p (QHD)"),
  R2160P(3840, 2160, "2160p (4K)"),
  R4320P(7680, 4320, "4320p (8K)");

  private final int width;
  private final int height;
  private final String displayName;

  VideoRes(int width, int height, String displayName) {
    this.width = width;
    this.height = height;
    this.displayName = displayName;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Get width and height as a record
   */
  public record Dimensions(int width, int height) {
  }

  public Dimensions toDimensions() {
    return new Dimensions(width, height);
  }

  /**
   * Get filename representation of resolution
   */
  public String toFileName() {
    return width + "x" + height;
  }

  /**
   * Get FFmpeg arguments for this resolution
   */
  public String toArgs() {
    return "-s " + width + "x" + height;
  }

  /**
   * Get list of common 16:9 resolutions
   */
  public static List<VideoRes> list169() {
    return List.of(R240P, R360P, R480P, R720P, R1080P, R1440P, R2160P, R4320P);
  }

  /**
   * Create VideoRes from width and height
   */
  public static VideoRes fromWH(int width, int height) {
    for (VideoRes res : values()) {
      if (res.width == width && res.height == height) {
        return res;
      }
    }
    // If no exact match found, return the closest one or custom implementation
    // TODO: Implement custom resolution support
    throw new IllegalArgumentException("Unsupported resolution: " + width + "x" + height);
  }

  /**
   * Create VideoRes from dynamic width/height with aspect ratio calculation
   * Port of Rust from_wh_dynamic method
   */
  public static Result<VideoRes, String> fromWHDynamic(
      Integer width,
      Integer height,
      VideoStreamInfo videoStream) {

    if (width == null && height == null) {
      return Result.err("Both width and height are null");
    }

    float ratio = (float) videoStream.width() / videoStream.height();
    System.out.println("ratio: " + ratio);

    int computedWidth;
    int computedHeight;

    if (width == null) {
      computedWidth = Math.round(height * ratio);
      computedHeight = height;
    } else if (height == null) {
      computedWidth = width;
      computedHeight = Math.round(width / ratio);
    } else {
      computedWidth = width;
      computedHeight = height;
    }

    try {
      VideoRes res = fromWH(computedWidth, computedHeight);
      return Result.ok(res);
    } catch (IllegalArgumentException e) {
      return Result.err(e.getMessage());
    }
  }

  /**
   * Basic video stream info record
   */
  public record VideoStreamInfo(int width, int height, double fps, String pixFmt) {
  }
}
