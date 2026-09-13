package ro.am.proiect.video_processor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.am.proiect.video_processor.model.VideoMetadata;

@Repository
public interface VideoRepository extends JpaRepository<VideoMetadata, Long> {
}