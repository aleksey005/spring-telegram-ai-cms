package ru.simshp.telegramexplorer.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="embedding")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmbeddingEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="message_id", nullable=false, unique=true)
    private MessageEntity message;

    /** Полный JSON-документ (см. docs/vector-json-schema.json) */
    @Column(name="json_payload", columnDefinition = "jsonb")
    private String jsonPayload;

    /** Вектор pgvector — просим Hibernate ничего «умного» не делать. */
    @Column(name="vector", columnDefinition = "vector(1536)")
    private float[] dummyVectorField; // не используется в ORM (будем вставлять через native SQL)
}
