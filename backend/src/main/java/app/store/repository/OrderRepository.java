package app.store.repository;

import app.store.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByIdAndUserId(String id, String userId);

    /**
     * SELECT ... FOR UPDATE: 2 request sửa/hủy cùng một đơn chạy lần lượt thay vì cùng lúc.
     * Chỉ lọc theo id: thêm điều kiện user.id thì Spring Data sinh LEFT JOIN users, Postgres không
     * khóa được qua outer join nên Hibernate chuyển sang khóa sau khi đọc (follow-on locking) và
     * request thứ 2 vẫn đọc trạng thái cũ. Kiểm tra chủ đơn làm ở service.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findForUpdateById(String id);
}
