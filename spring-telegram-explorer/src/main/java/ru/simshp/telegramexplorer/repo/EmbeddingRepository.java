package ru.simshp.telegramexplorer.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.simshp.telegramexplorer.domain.EmbeddingEntity;

public interface EmbeddingRepository extends JpaRepository<EmbeddingEntity, Long> { }
