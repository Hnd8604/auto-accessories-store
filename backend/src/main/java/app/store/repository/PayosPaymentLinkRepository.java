package app.store.repository;

import app.store.entity.PayosPaymentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PayosPaymentLinkRepository extends JpaRepository<PayosPaymentLink, Long> {

    Optional<PayosPaymentLink> findByPayosOrderCode(Long payosOrderCode);

    Optional<PayosPaymentLink> findFirstByOrderIdOrderByIdDesc(String orderId);

    @Query(value = "SELECT nextval('payos_order_code_seq')", nativeQuery = true)
    long nextPayosOrderCode();
}
