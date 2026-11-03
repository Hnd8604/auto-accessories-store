package app.store.repository;

import app.store.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    Page<Product> findProductsByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId")
    Page<Product> findProductsByBrandId(@Param("brandId") Long brandId, Pageable pageable);

    /**
     * SELECT ... FOR UPDATE các product, sắp theo id. Dùng khi đổi tồn kho để 2 đơn cùng lúc
     * không cùng đọc một số tồn rồi cùng trừ. Luôn khóa theo thứ tự id để tránh deadlock.
     * Phải gọi trước khi product được load trong transaction, nếu không Hibernate trả về
     * bản đã load sẵn (có thể đã cũ).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Product> findForUpdateByIdInOrderByIdAsc(Collection<Long> ids);

    boolean existsBySlug(String slug);
    Optional<Product> findBySlug(String slug);

    /**
     * Fetch all products with their images eagerly to avoid N+1 queries.
     * Uses a separate countQuery because JOIN FETCH is not allowed in count queries with pagination.
     */
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productImages",
           countQuery = "SELECT COUNT(p) FROM Product p")
    Page<Product> findAllWithImages(Pageable pageable);

    @Query("SELECT DISTINCT p.category.id AS categoryId, p.brand.id AS brandId FROM Product p WHERE p.category IS NOT NULL AND p.brand IS NOT NULL")
    List<CategoryBrandPair> findDistinctCategoryBrandPairs();
}
