package wappon28dev.vvncv_java.test;

import wappon28dev.vvncv_java.modules.*;
import wappon28dev.vvncv_java.util.Result;

import java.io.IOException;

/**
 * Tests for video processing functionality
 * Simple test without JUnit dependencies
 */
public class SimpleVideoTest {

  public static void main(String[] args) throws IOException {
    SimpleVideoTest test = new SimpleVideoTest();

    System.out.println("=== Running Simple Video Tests ===");

    test.testResultType();
    test.testFileModule();
    test.testVideoResolutionConversion();
    test.testVideoStatExtraction();
    test.testVideoConfigValidation();

    System.out.println("=== All tests completed ===");
  }

  void testVideoStatExtraction() throws IOException {
    System.out.println("\n--- Testing Video Stat Extraction ---");

    VideoModule videoModule = new VideoModule();
    Result<VideoStat, String> result = videoModule.stat("assets/01.mp4");

    if (result.isErr()) {
      System.err.println("Warning: Could not load test video - " + result.unwrapOr(null));
      return;
    }

    VideoStat stat = result.unwrap();

    assert stat != null : "Video stat should not be null";
    assert stat.videoStream() != null : "Video stream should not be null";
    assert stat.videoStream().width() > 0 : "Width should be positive";
    assert stat.videoStream().height() > 0 : "Height should be positive";
    assert stat.videoStream().fps() > 0 : "FPS should be positive";
    assert stat.duration().toMillis() > 0 : "Duration should be positive";
    assert stat.fileSize() > 0 : "File size should be positive";

    System.out.println("✓ Video stats extracted successfully:");
    System.out.println("  Resolution: " + stat.videoStream().width() + "x" + stat.videoStream().height());
    System.out.println("  FPS: " + stat.videoStream().fps());
    System.out.println("  Duration: " + stat.duration().toSeconds() + "s");
    System.out.println("  File size: " + stat.fileSize() + " bytes");
    System.out.println("  Audio streams: " + stat.audioStreams().size());
  }

  void testVideoResolutionConversion() {
    System.out.println("\n--- Testing Video Resolution Conversion ---");

    VideoRes res720p = VideoRes.R720P;
    assert res720p.getWidth() == 1280 : "720p width should be 1280";
    assert res720p.getHeight() == 720 : "720p height should be 720";
    assert "1280x720".equals(res720p.toFileName()) : "Filename should be 1280x720";
    assert "-s 1280x720".equals(res720p.toArgs()) : "Args should be -s 1280x720";

    var resList = VideoRes.list169();
    assert resList.contains(VideoRes.R720P) : "List should contain 720p";
    assert resList.contains(VideoRes.R1080P) : "List should contain 1080p";

    System.out.println("✓ VideoRes tests passed");
  }

  void testVideoConfigValidation() {
    System.out.println("\n--- Testing Video Config Validation ---");

    var videoStream = new VideoStat.VideoStreamInfo(1920, 1080, 30.0, "yuv420p");
    var audioStreams = java.util.List.<VideoStat.AudioStreamInfo>of();
    var stat = new VideoStat(
        "test.mp4",
        videoStream,
        audioStreams,
        java.time.Duration.ofSeconds(60),
        1000000L);

    // Test valid config
    VideoConfig validConfig = new VideoConfig(VideoRes.R720P, 30, 23, false);
    Result<Void, String> result = validConfig.checkUpScaling(stat);
    assert result.isOk() : "Valid config should pass";

    // Test upscaling resolution
    VideoConfig invalidResConfig = new VideoConfig(VideoRes.R2160P, 30, 23, false);
    result = invalidResConfig.checkUpScaling(stat);
    assert result.isErr() : "Invalid resolution config should fail";

    // Test upscaling FPS
    VideoConfig invalidFpsConfig = new VideoConfig(VideoRes.R720P, 60, 23, false);
    result = invalidFpsConfig.checkUpScaling(stat);
    assert result.isErr() : "Invalid FPS config should fail";

    System.out.println("✓ VideoConfig validation tests passed");
  }

  void testFileModule() {
    System.out.println("\n--- Testing File Module ---");

    var parts = FileModule.getFileName("path/to/video.mp4");
    assert "video".equals(parts.name()) : "Name should be 'video'";
    assert "mp4".equals(parts.extension()) : "Extension should be 'mp4'";

    var partsNoExt = FileModule.getFileName("video");
    assert "video".equals(partsNoExt.name()) : "Name should be 'video'";
    assert "".equals(partsNoExt.extension()) : "Extension should be empty";

    System.out.println("✓ FileModule tests passed");
  }

  void testResultType() {
    System.out.println("\n--- Testing Result Type ---");

    Result<String, String> okResult = Result.ok("success");
    assert okResult.isOk() : "OK result should be ok";
    assert !okResult.isErr() : "OK result should not be err";
    assert "success".equals(okResult.unwrap()) : "OK result should unwrap to success";

    Result<String, String> errResult = Result.err("error");
    assert errResult.isErr() : "Err result should be err";
    assert !errResult.isOk() : "Err result should not be ok";
    assert "default".equals(errResult.unwrapOr("default")) : "Err result should use default";

    // Test map
    Result<Integer, String> mappedResult = okResult.map(String::length);
    assert mappedResult.isOk() : "Mapped result should be ok";
    assert mappedResult.unwrap() == 7 : "Mapped result should be 7";

    System.out.println("✓ Result type tests passed");
  }
}
