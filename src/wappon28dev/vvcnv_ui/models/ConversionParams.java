package wappon28dev.vvcnv_ui.models;

import wappon28dev.vvcnv_java.modules.VideoRes;

/**
 * Parameters for video conversion
 */
public record ConversionParams(
    String inputPath,
    String outputDir,
    boolean hasAudio,
    String encoding,
    VideoRes minRes,
    VideoRes maxRes,
    int resSteps,
    int minCrf,
    int maxCrf,
    int crfSteps,
    int maxThreads) {
}
