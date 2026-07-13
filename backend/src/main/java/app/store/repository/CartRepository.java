package app.store.repository;

import app.store.entity.Cart;
import app.store.entity.CartItem;
import app.store.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(String userId);

    Optional<Cart> findByUser_Username(String username);
}
