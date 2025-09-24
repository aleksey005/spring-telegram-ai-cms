package ru.simshp.telegramexplorer.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.simshp.telegramexplorer.domain.AiCommentEntity;

import java.util.Optional;

public interface AiCommentRepository extends JpaRepository<AiCommentEntity, Long> {
    Optional<AiCommentEntity> findByMessage_Id(Long messageId);
}
