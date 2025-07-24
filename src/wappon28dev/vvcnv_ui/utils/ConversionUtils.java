package wappon28dev.vvcnv_ui.utils;

import wappon28dev.vvcnv_java.modules.VideoRes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class ConversionUtils {

  private ConversionUtils() {
    // Utility class
  }

  /**
   * Generate resolution list based on parameters
   */
  public static List<VideoRes> generateResolutions(VideoRes minRes, VideoRes maxRes, int resSteps) {
    var allRes = VideoRes.list169();
    int minIndex = allRes.indexOf(minRes);
    int maxIndex = allRes.indexOf(maxRes);

    if (resSteps == 1) {
      return List.of(maxRes);
    }

    var result = new ArrayList<VideoRes>();
    for (int i = 0; i < resSteps; i++) {
      int index = minIndex + (int) Math.round((double) i * (maxIndex - minIndex) / (resSteps - 1));
      result.add(allRes.get(index));
    }
    return result;
  }

  /**
   * Generate CRF values based on parameters (descending order for better intuition)
   */
  public static List<Integer> generateCrfValues(int minCrf, int maxCrf, int crfSteps) {
    if (crfSteps == 1) {
      return List.of(maxCrf);
    }

    return IntStream.range(0, crfSteps)
        .map(i -> maxCrf - (int) Math.round((double) i * (maxCrf - minCrf) / (crfSteps - 1)))
        .boxed()
        .toList();
  }
}
