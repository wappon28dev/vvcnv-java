package wappon28dev.vvcnv_ui.models;

/**
 * Conversion result data
 */
public record ConversionResult(
    boolean success,
    String fileSize,
    String outputPath,
    String error,
    double fileSizeMB) {
}
