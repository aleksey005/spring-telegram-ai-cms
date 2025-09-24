package ru.simshp.telegramexplorer.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.simshp.telegramexplorer.domain.MediaEntity;

import java.util.Optional;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {

    Optional<MediaEntity> findFirstByMessage_IdAndKindOrderByIdAsc(Long messageId, String kind);
}
