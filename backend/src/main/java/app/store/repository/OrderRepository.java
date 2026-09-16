package app.store.repository;

import app.store.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByIdAndUserId(String id, String userId);
}
