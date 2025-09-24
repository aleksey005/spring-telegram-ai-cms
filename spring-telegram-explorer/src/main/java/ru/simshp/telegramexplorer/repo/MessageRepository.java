package ru.simshp.telegramexplorer.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.simshp.telegramexplorer.domain.MessageEntity;

import java.util.Collection;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    Optional<MessageEntity> findByTgChatIdAndTgMessageId(Long chatId, Long messageId);

    @EntityGraph(attributePaths = {"channel", "aiComment"})
    Page<MessageEntity> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"channel", "aiComment"})
    Page<MessageEntity> findAllByChannel_UsernameIn(Collection<String> usernames, Pageable pageable);
}
