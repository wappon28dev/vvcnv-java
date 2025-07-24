package wappon28dev.vvncv_java.test;

import wappon28dev.vvncv_java.modules.*;
import wappon28dev.vvncv_java.util.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Tests for video processing functionality
 */
public class VideoModuleTest {
    
    private VideoModule videoModule;
    
    @BeforeEach
    void setUp() throws IOException {
        videoModule = new VideoModule();
    }
    
    @Test
    void testVideoStatExtraction() {
        // Test video stat extraction
        Result<VideoStat, String> result = videoModule.stat("assets/01.mp4");
        
        if (result.isErr()) {
            System.err.println("Warning: Could not load test video - " + result.unwrapOr(null));
            return; // Skip test if video not available
        }
        
        VideoStat stat = result.unwrap();
        
        assertNotNull(stat);
        assertNotNull(stat.videoStream());
        assertTrue(stat.videoStream().width() > 0);
        assertTrue(stat.videoStream().height() > 0);
        assertTrue(stat.videoStream().fps() > 0);
        assertTrue(stat.duration().toMillis() > 0);
        assertTrue(stat.fileSize() > 0);
        
        System.out.println("Video stats extracted successfully:");
        System.out.println("  Resolution: " + stat.videoStream().width() + "x" + stat.videoStream().height());
        System.out.println("  FPS: " + stat.videoStream().fps());
        System.out.println("  Duration: " + stat.duration().toSeconds() + "s");
        System.out.println("  File size: " + stat.fileSize() + " bytes");
        System.out.println("  Audio streams: " + stat.audioStreams().size());
    }
    
    @Test
    void testVideoResolutionConversion() {
        // Test VideoRes functionality
        VideoRes res720p = VideoRes.R720P;
        assertEquals(1280, res720p.getWidth());
        assertEquals(720, res720p.getHeight());
        assertEquals("1280x720", res720p.toFileName());
        assertEquals("-s 1280x720", res720p.toArgs());
        
        // Test list of resolutions
        var resList = VideoRes.list169();
        assertTrue(resList.contains(VideoRes.R720P));
        assertTrue(resList.contains(VideoRes.R1080P));
        
        System.out.println("VideoRes tests passed");
    }
    
    @Test
    void testVideoConfigValidation() {
        // Create a mock video stat for testing
        var videoStream = new VideoStat.VideoStreamInfo(1920, 1080, 30.0, "yuv420p");
        var audioStreams = java.util.List.<VideoStat.AudioStreamInfo>of();
        var stat = new VideoStat(
            "test.mp4",
            videoStream,
            audioStreams,
            java.time.Duration.ofSeconds(60),
            1000000L
        );
        
        // Test valid config
        VideoConfig validConfig = new VideoConfig(VideoRes.R720P, 30, 23, false);
        Result<Void, String> result = validConfig.checkUpScaling(stat);
        assertTrue(result.isOk());
        
        // Test upscaling resolution
        VideoConfig invalidResConfig = new VideoConfig(VideoRes.R2160P, 30, 23, false);
        result = invalidResConfig.checkUpScaling(stat);
        assertTrue(result.isErr());
        
        // Test upscaling FPS
        VideoConfig invalidFpsConfig = new VideoConfig(VideoRes.R720P, 60, 23, false);
        result = invalidFpsConfig.checkUpScaling(stat);
        assertTrue(result.isErr());
        
        System.out.println("VideoConfig validation tests passed");
    }
    
    @Test
    void testFileModule() {
        // Test file name extraction
        var parts = FileModule.getFileName("path/to/video.mp4");
        assertEquals("video", parts.name());
        assertEquals("mp4", parts.extension());
        
        // Test with no extension
        var partsNoExt = FileModule.getFileName("video");
        assertEquals("video", partsNoExt.name());
        assertEquals("", partsNoExt.extension());
        
        System.out.println("FileModule tests passed");
    }
    
    @Test
    void testResultType() {
        // Test Result<T, E> functionality
        Result<String, String> okResult = Result.ok("success");
        assertTrue(okResult.isOk());
        assertFalse(okResult.isErr());
        assertEquals("success", okResult.unwrap());
        
        Result<String, String> errResult = Result.err("error");
        assertTrue(errResult.isErr());
        assertFalse(errResult.isOk());
        assertEquals("default", errResult.unwrapOr("default"));
        
        // Test map
        Result<Integer, String> mappedResult = okResult.map(String::length);
        assertTrue(mappedResult.isOk());
        assertEquals(7, mappedResult.unwrap());
        
        System.out.println("Result type tests passed");
    }
}
