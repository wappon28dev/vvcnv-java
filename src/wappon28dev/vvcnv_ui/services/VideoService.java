package wappon28dev.vvcnv_ui.services;

import wappon28dev.vvncv_java.modules.VideoModule;
import wappon28dev.vvncv_java.modules.VideoStat;
import wappon28dev.vvncv_java.util.Result;

import javax.swing.*;
import java.io.IOException;

/**
 * Service for video processing operations
 */
public class VideoService {

  private final VideoModule videoModule;

  public VideoService() throws IOException {
    this.videoModule = new VideoModule();
  }

  /**
   * Load video statistics asynchronously
   */
  public SwingWorker<VideoStat, Void> loadVideoStatsAsync(String inputPath) {
    return new SwingWorker<>() {
      @Override
      protected VideoStat doInBackground() throws Exception {
        System.out.println("動画統計情報を読み込み中: " + inputPath);
        Result<VideoStat, String> result = videoModule.stat(inputPath);

        return switch (result) {
          case Result.Ok<VideoStat, String> ok -> {
            System.out.println("動画統計情報の読み込み完了");
            yield ok.value();
          }
          case Result.Err<VideoStat, String> err -> {
            System.err.println("VideoModule.stat() エラー: " + err.error());
            throw new RuntimeException("VideoModule.stat() failed: " + err.error());
          }
        };
      }
    };
  }

  /**
   * Get the video module instance
   */
  public VideoModule getVideoModule() {
    return videoModule;
  }
}
