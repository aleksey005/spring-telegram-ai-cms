package ru.simshp.telegramexplorer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "message",
       indexes = {@Index(name="idx_message_channel_time", columnList = "channel_id,published_at")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="tg_chat_id", nullable=false)
    private Long tgChatId;

    @Column(name="tg_message_id", nullable=false)
    private Long tgMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="channel_id", nullable=false)
    private ChannelEntity channel;

    @Column(name="thread_id")
    private Long threadId; // для комментариев

    @Column(name="author_username")
    private String authorUsername;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name="has_media")
    private boolean hasMedia;

    @Column(name="is_comment")
    private boolean comment;

    @Column(name="published_at")
    private OffsetDateTime publishedAt;

    @OneToOne(mappedBy = "message", fetch = FetchType.LAZY)
    private AiCommentEntity aiComment;
}
