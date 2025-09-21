package ru.simshp.telegramexplorer.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "channel")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChannelEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String username;

    private String title;
}
