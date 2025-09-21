package ru.simshp.telegramexplorer.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MediaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="message_id", nullable=false)
    private MessageEntity message;

    private String kind;      // photo|video|document|other
    private String mimeType;

    @Column(name="file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String caption;
}
