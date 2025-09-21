package ru.simshp.telegramexplorer.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.simshp.telegramexplorer.domain.ChannelEntity;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<ChannelEntity, Long> {
    Optional<ChannelEntity> findByUsername(String username);
}
