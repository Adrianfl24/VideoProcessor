package ro.am.proiect.video_processor.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("ro.am.proiect.video_processor.repository")
@EntityScan("ro.am.proiect.video_processor.model")
public class VideoProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoProcessorApplication.class, args);
    }
}



