package app.store.entity;

import app.store.enums.ConversationChannel;
import app.store.enums.ConversationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String guestName;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    ConversationChannel channel = ConversationChannel.WEB;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    ConversationStatus status = ConversationStatus.OPEN;

    @Column(nullable = false)
    @Builder.Default
    int unreadCount = 0;

    LocalDateTime lastMessageAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    LocalDateTime updatedAt;
}
