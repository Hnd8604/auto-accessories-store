package app.store.repository;

import app.store.entity.Conversation;
import app.store.enums.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    Page<Conversation> findAllByOrderByLastMessageAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.unreadCount), 0) FROM Conversation c WHERE c.status = :status")
    long sumTotalUnread(@Param("status") ConversationStatus status);
}
