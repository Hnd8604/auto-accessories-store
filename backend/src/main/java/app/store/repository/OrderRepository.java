package app.store.repository;

import app.store.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    @Query("SELECT o FROM Order o WHERE o.user.username = :username")
    List<Order> getOrderByUserName(@Param("username") String username);

    Optional<Order> findByOrderCode(String orderCode);
}
