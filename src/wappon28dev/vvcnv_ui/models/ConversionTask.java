package wappon28dev.vvcnv_ui.models;

import wappon28dev.vvncv_java.modules.VideoConfig;

/**
 * Conversion task data
 */
public record ConversionTask(
    VideoConfig config,
    int resIndex,
    int crfIndex) {
}
