package ru.simshp.telegramexplorer.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.simshp.telegramexplorer.domain.MediaEntity;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> { }
