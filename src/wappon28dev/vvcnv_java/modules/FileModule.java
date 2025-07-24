package wappon28dev.vvcnv_java.modules;

import wappon28dev.vvcnv_java.util.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File utility module - port of Rust file.rs
 */
public class FileModule {

  /**
   * Calculate file size in bytes
   * 
   * @param path The file path
   * @return Result containing file size or IOException
   */
  public static Result<Long, IOException> calcSize(String path) {
    try {
      Path filePath = Paths.get(path);
      long size = Files.size(filePath);
      return Result.ok(size);
    } catch (IOException e) {
      return Result.err(e);
    }
  }

  /**
   * Extract filename and extension from path
   * 
   * @param path The file path
   * @return Record containing filename and extension
   */
  public static FileNameParts getFileName(String path) {
    String fileName = Paths.get(path).getFileName().toString();
    int lastDotIndex = fileName.lastIndexOf('.');

    if (lastDotIndex == -1) {
      return new FileNameParts(fileName, "");
    }

    String nameWithoutExt = fileName.substring(0, lastDotIndex);
    String extension = fileName.substring(lastDotIndex + 1);

    return new FileNameParts(nameWithoutExt, extension);
  }

  /**
   * Record to hold filename parts
   * 
   * @param name      The filename without extension
   * @param extension The file extension
   */
  public record FileNameParts(String name, String extension) {
  }
}
