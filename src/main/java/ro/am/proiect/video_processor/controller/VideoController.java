package ro.am.proiect.video_processor.controller;

import com.azure.storage.blob.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.am.proiect.video_processor.model.VideoMetadata;
import ro.am.proiect.video_processor.repository.VideoRepository;

import java.io.File;

@Controller
@RequestMapping("/videos")
public class VideoController {

    @Autowired
    private VideoRepository videoRepository;

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @GetMapping
    public String showGallery(Model model) {

        model.addAttribute("videos", videoRepository.findAll());
        return "index";
    }

    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) {

        if (file.isEmpty()) {
            model.addAttribute("error", "Alege un fisier!");
            model.addAttribute("videos", videoRepository.findAll());
            return "index";
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            model.addAttribute("error", "Format neacceptat! Trebuie sa incarci doar fisiere video.");
            model.addAttribute("videos", videoRepository.findAll());
            return "index";
        }

        File rawFile = null;
        File thumbFile = null;
        File finalVideo = null;

        try {
            String baseName = "vid_" + System.currentTimeMillis();
            rawFile = File.createTempFile(baseName + "_raw", ".tmp");
            thumbFile = new File(System.getProperty("java.io.tmpdir"), baseName + "_thumb.jpg");
            finalVideo = new File(System.getProperty("java.io.tmpdir"), baseName + "_final.mp4");

            file.transferTo(rawFile);

            processVideoWithCover(rawFile, finalVideo, thumbFile);

            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString).buildClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            BlobClient videoBlob = containerClient.getBlobClient(finalVideo.getName());
            videoBlob.uploadFromFile(finalVideo.getAbsolutePath(), true);
            String finalVideoUrl = videoBlob.getBlobUrl();

            BlobClient thumbBlob = containerClient.getBlobClient(thumbFile.getName());
            thumbBlob.uploadFromFile(thumbFile.getAbsolutePath(), true);
            String thumbUrl = thumbBlob.getBlobUrl();

            VideoMetadata metadata = new VideoMetadata(file.getOriginalFilename(), finalVideoUrl, thumbUrl);
            videoRepository.save(metadata);

            return "redirect:/videos";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Eroare la procesare");
            model.addAttribute("videos", videoRepository.findAll());
            return "index";
        } finally {
            if (rawFile != null && rawFile.exists()) rawFile.delete();
            if (thumbFile != null && thumbFile.exists()) thumbFile.delete();
            if (finalVideo != null && finalVideo.exists()) finalVideo.delete();
        }
    }

    private void processVideoWithCover(File rawInput, File finalOutput, File thumbnailOutput) throws Exception {

        String ffmpegPath = "C:\\ffmpeg\\bin\\ffmpeg.exe";

        ProcessBuilder extractPb = new ProcessBuilder(
                ffmpegPath, "-i", rawInput.getAbsolutePath(),
                "-vf", "thumbnail=100,scale=480:-1",
                "-frames:v", "1", "-y", thumbnailOutput.getAbsolutePath()
        );
        extractPb.inheritIO().start().waitFor();

        ProcessBuilder mergePb = new ProcessBuilder(
                ffmpegPath,
                "-i", rawInput.getAbsolutePath(),
                "-i", thumbnailOutput.getAbsolutePath(),
                "-map", "0:v", "-map", "0:a", "-map", "1",
                "-c:v:0", "libx264", "-crf", "23", "-c:a", "aac",
                "-c:v:1", "mjpeg", "-disposition:v:1", "attached_pic",
                "-y", finalOutput.getAbsolutePath()
        );
        mergePb.inheritIO().start().waitFor();
    }
}