package wappon28dev.vvcnv_java;

import wappon28dev.vvcnv_java.modules.*;
import wappon28dev.vvcnv_java.util.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  private static final String INPUT_PATH = "assets/01.mp4";
  private static final String OUTPUT_DIR = "out";

  public static void main(String[] args) throws IOException {
    System.out.println("Welcome to VVNCV Java!");

    // Ensure output directory exists
    Files.createDirectories(Paths.get(OUTPUT_DIR));

    // Initialize video module
    VideoModule videoModule = new VideoModule();

    // Get video statistics
    Result<VideoStat, String> statResult = videoModule.stat(INPUT_PATH);
    if (statResult.isErr()) {
      System.err.println("Failed to get video stats: " + statResult.unwrapOr(null));
      return;
    }

    VideoStat stat = statResult.unwrap();
    System.out.println("Original video size: " + formatFileSize(stat.fileSize()));
    System.out.println("Video info: " + stat.videoStream().width() + "x" + stat.videoStream().height() +
        " @ " + stat.videoStream().fps() + "fps");
    System.out.println("Duration: " + formatDuration(stat.duration().toSeconds()));
    System.out.println("Audio streams: " + stat.audioStreams().size());

    // Define encoding parameters (simplified version of Rust's iproduct!)
    List<VideoRes> resolutions = VideoRes.list169();

    List<Integer> fpsList = List.of(30);
    List<Integer> crfList = List.of(20, 40);

    // Create thread pool for parallel processing
    ExecutorService executor = Executors.newFixedThreadPool(4);

    // Process videos in parallel
    var futures = resolutions.stream()
        .flatMap(res -> fpsList.stream()
            .flatMap(fps -> crfList.stream()
                .map(crf -> {
                  VideoConfig config = new VideoConfig(res, fps, crf, true);
                  return CompletableFuture.supplyAsync(() -> processVideo(videoModule, stat, config), executor);
                })))
        .toList();

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> {
          System.out.println("\n✓ All encoding tasks completed!");

          // Show results
          long successCount = futures.stream()
              .mapToLong(f -> f.join().isOk() ? 1 : 0)
              .sum();
          long totalCount = futures.size();

          System.out.println("Success: " + successCount + "/" + totalCount);

          // Show failures
          futures.stream()
              .filter(f -> f.join().isErr())
              .forEach(f -> {
                var result = f.join();
                System.err.println("Error: " + ((Result.Err<?, String>) result).error());
              });

          executor.shutdown();
        })
        .join();
  }

  private static Result<Void, String> processVideo(VideoModule videoModule, VideoStat stat, VideoConfig config) {
    try {
      var fileNameParts = FileModule.getFileName(stat.path());
      String outputPath = OUTPUT_DIR + "/" + fileNameParts.name() + config.toFileName() + "."
          + fileNameParts.extension();

      System.out
          .println("Starting: " + config.res().getDisplayName() + " FPS:" + config.fps() + " CRF:" + config.crf());

      var params = new VideoModule.VideoProcessParams(outputPath, config);
      var result = videoModule.processSimple(stat, params);

      if (result.isOk()) {
        // Get output file size
        var sizeResult = FileModule.calcSize(outputPath);
        if (sizeResult.isOk()) {
          long outputSize = sizeResult.unwrap();
          System.out.println("✓ Completed: " + outputPath + " (" + formatFileSize(outputSize) + ")");
        }
      }

      return result;

    } catch (Exception e) {
      return Result.err("Processing failed: " + e.getMessage());
    }
  }

  private static String formatFileSize(long bytes) {
    if (bytes < 1024)
      return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "";
    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
  }

  private static String formatDuration(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, secs);
  }
}
