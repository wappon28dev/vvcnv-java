package wappon28dev.vvncv_java.modules;

import wappon28dev.vvncv_java.util.Result;
import wappon28dev.vvncv_java.modules.VideoStat.VideoStreamInfo;
import wappon28dev.vvncv_java.modules.VideoStat.AudioStreamInfo;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Video processing module - port of Rust video.rs
 */
public class VideoModule {

  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final FFmpegExecutor executor;

  public VideoModule() throws IOException {
    // Try different FFmpeg installation paths
    String[] possiblePaths = {
        "/opt/homebrew/bin/ffmpeg", // Apple Silicon Homebrew
        "/usr/local/bin/ffmpeg", // Intel Homebrew
        "/usr/bin/ffmpeg", // System install
        "ffmpeg" // PATH
    };

    String[] possibleProbePaths = {
        "/opt/homebrew/bin/ffprobe", // Apple Silicon Homebrew
        "/usr/local/bin/ffprobe", // Intel Homebrew
        "/usr/bin/ffprobe", // System install
        "ffprobe" // PATH
    };

    FFmpeg ffmpegTmp = null;
    FFprobe ffprobeTmp = null;

    // Try to find ffmpeg
    for (String path : possiblePaths) {
      try {
        ffmpegTmp = new FFmpeg(path);
        System.out.println("FFmpeg見つかりました: " + path);
        break;
      } catch (IOException e) {
        System.out.println("FFmpegが見つかりません: " + path);
      }
    }

    // Try to find ffprobe
    for (String path : possibleProbePaths) {
      try {
        ffprobeTmp = new FFprobe(path);
        System.out.println("FFprobe見つかりました: " + path);
        break;
      } catch (IOException e) {
        System.out.println("FFprobeが見つかりません: " + path);
      }
    }

    if (ffmpegTmp == null) {
      throw new IOException("FFmpegが見つかりません。以下を確認してください：\n" +
          "1. Homebrewでインストール: brew install ffmpeg\n" +
          "2. パスが通っているか確認: which ffmpeg");
    }

    if (ffprobeTmp == null) {
      throw new IOException("FFprobeが見つかりません。FFmpegと一緒にインストールされているはずです。");
    }

    this.ffmpeg = ffmpegTmp;
    this.ffprobe = ffprobeTmp;
    this.executor = new FFmpegExecutor(ffmpeg, ffprobe);
  }

  /**
   * Get video statistics - port of Rust stat function
   */
  public Result<VideoStat, String> stat(String inputPath) {
    try {
      System.out.println("FFprobe実行中: " + inputPath);
      FFmpegProbeResult probeResult = ffprobe.probe(inputPath);

      // Extract video stream
      FFmpegStream videoStream = probeResult.getStreams().stream()
          .filter(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO)
          .findFirst()
          .orElse(null);

      if (videoStream == null) {
        return Result.err("No video stream found");
      }

      System.out.println("動画ストリーム情報: " + videoStream.width + "x" + videoStream.height);

      // Extract audio streams
      List<AudioStreamInfo> audioStreams = probeResult.getStreams().stream()
          .filter(stream -> stream.codec_type == FFmpegStream.CodecType.AUDIO)
          .map(stream -> new AudioStreamInfo(
              stream.codec_name,
              stream.sample_rate,
              stream.channels))
          .toList();

      System.out.println("音声ストリーム数: " + audioStreams.size());

      // Get duration with error handling
      Duration duration;
      try {
        double durationValue = probeResult.format.duration;
        if (durationValue > 0) {
          duration = Duration.ofNanos((long) (durationValue * 1_000_000_000));
        } else {
          System.err.println("警告: 動画の長さ情報が無効です: " + durationValue);
          duration = Duration.ZERO;
        }
      } catch (Exception e) {
        System.err.println("動画の長さ変換エラー: " + e.getMessage());
        e.printStackTrace();
        duration = Duration.ZERO;
      }

      // Get file size with error handling
      long fileSize;
      try {
        fileSize = probeResult.format.size;
        if (fileSize < 0) {
          System.err.println("警告: ファイルサイズが無効です: " + fileSize);
          fileSize = 0L;
        }
      } catch (Exception e) {
        System.err.println("ファイルサイズ取得エラー: " + e.getMessage());
        e.printStackTrace();
        fileSize = 0L;
      }

      // Create video stream info with error handling
      VideoStreamInfo videoInfo;
      try {
        double fps = 0.0;
        if (videoStream.r_frame_rate != null) {
          try {
            fps = videoStream.r_frame_rate.doubleValue();
          } catch (Exception e) {
            System.err.println("フレームレート変換エラー: " + e.getMessage());
            System.err.println("r_frame_rate値: " + videoStream.r_frame_rate);
            fps = 0.0;
          }
        } else {
          System.err.println("警告: フレームレート情報がnullです");
        }

        System.out.println("検出されたフレームレート: " + fps);

        videoInfo = new VideoStreamInfo(
            videoStream.width,
            videoStream.height,
            fps,
            videoStream.pix_fmt);
      } catch (Exception e) {
        System.err.println("動画ストリーム情報作成エラー: " + e.getMessage());
        e.printStackTrace();
        return Result.err("Failed to create video stream info: " + e.getMessage());
      }

      VideoStat stat = new VideoStat(
          inputPath,
          videoInfo,
          audioStreams,
          duration,
          fileSize);

      System.out.println("動画統計情報作成完了");
      return Result.ok(stat);

    } catch (IOException e) {
      return Result.err("Failed to probe video: " + e.getMessage());
    }
  }

  /**
   * Process video with given configuration - port of Rust process function
   */
  public Result<Void, String> process(VideoStat stat, VideoProcessParams params) throws IOException {
    var config = params.config();
    var outputPath = params.outputPath();

    // Check for upscaling
    var upscalingCheck = config.checkUpScaling(stat);
    if (upscalingCheck.isErr()) {
      return upscalingCheck;
    }

    System.out.println("Starting encoding: " + outputPath);
    System.out.println("Config: " + config);

    // Build FFmpeg command
    var outputBuilder = new FFmpegBuilder()
        .setInput(stat.path())
        .overrideOutputFiles(true)
        .addOutput(outputPath)
        .setVideoCodec("libx264")
        .setVideoResolution(config.res().getWidth(), config.res().getHeight())
        .setVideoFrameRate(config.fps())
        .setConstantRateFactor(config.crf())
        .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL);

    // Add audio if required
    if (config.hasAudio() && !stat.audioStreams().isEmpty()) {
      outputBuilder.setAudioCodec("aac");
    } else {
      outputBuilder.setAudioCodec("none");
    }

    FFmpegBuilder builder = outputBuilder.done();

    FFmpegJob job = executor.createJob(builder, progress -> {
      double durationNs = stat.duration().toNanos();
      double progressPercent = progress.out_time_ns / durationNs * 100;

      System.out.printf("\rProgress: %.2f%% (Frame: %d, Time: %s)%n",
          progressPercent,
          progress.frame,
          formatDuration(progress.out_time_ns / 1_000_000_000.0));
    });

    try {
      job.run();
      System.out.println("\n✓ Encoding completed: " + outputPath);
      return Result.ok(null);
    } catch (Exception e) {
      return Result.err("Encoding failed: " + e.getMessage());
    }
  }

  /**
   * Process video without progress monitoring (safer for some FFmpeg versions)
   */
  public Result<Void, String> processSimple(VideoStat stat, VideoProcessParams params) {
    try {
      var config = params.config();
      var outputPath = params.outputPath();

      // Check for upscaling
      var upscalingCheck = config.checkUpScaling(stat);
      if (upscalingCheck.isErr()) {
        return upscalingCheck;
      }

      System.out.println("Starting encoding: " + outputPath);
      System.out.println("Config: " + config);

      // Build FFmpeg command
      var outputBuilder = new FFmpegBuilder()
          .setInput(stat.path())
          .overrideOutputFiles(true)
          .addOutput(outputPath)
          .setVideoCodec("libx264")
          .setVideoResolution(config.res().getWidth(), config.res().getHeight())
          .setVideoFrameRate(config.fps())
          .setConstantRateFactor(config.crf())
          .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL);

      // Add audio if required
      if (config.hasAudio() && !stat.audioStreams().isEmpty()) {
        outputBuilder.setAudioCodec("aac");
      } else {
        outputBuilder.setAudioCodec("none");
      }

      FFmpegBuilder builder = outputBuilder.done();

      // Run without progress monitoring
      FFmpegJob job = executor.createJob(builder);
      job.run();
      System.out.println("✓ Encoding completed: " + outputPath);
      return Result.ok(null);
    } catch (Exception e) {
      System.err.println("エンコーディングエラー: " + e.getMessage());
      e.printStackTrace();
      return Result.err("Encoding failed: " + e.getMessage());
    }
  }

  private String formatDuration(double seconds) {
    long hours = (long) (seconds / 3600);
    long minutes = (long) ((seconds % 3600) / 60);
    long secs = (long) (seconds % 60);
    return String.format("%02d:%02d:%02d", hours, minutes, secs);
  }

  /**
   * Video processing parameters record
   */
  public record VideoProcessParams(String outputPath, VideoConfig config) {
  }
}
