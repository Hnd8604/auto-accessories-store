package app.store.repository;

import app.store.entity.Order;
import app.store.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @Query("SELECT p FROM ProductImage p WHERE p.product.id = :productId")
    List<ProductImage> getProductImageByProductId(@Param("productId") Long productId);

    /**
     * Bước 1: Đặt lại (reset) tất cả ảnh của một sản phẩm về is_primary = false.
     * Chúng ta dùng JPQL (gần giống SQL nhưng làm việc với Entity).
     */
    @Modifying
    @Query("UPDATE ProductImage p SET p.isPrimary = false WHERE p.product.id = :productId")
    void resetAllPrimaryImagesForProduct(@Param("productId") Long productId);

    /**
     * Bước 2: Đặt ảnh được chỉ định làm ảnh chính.
     */
    @Modifying
    @Query("UPDATE ProductImage p SET p.isPrimary = true WHERE p.id = :imageId AND p.product.id = :productId")
    void setNewPrimaryImage(@Param("imageId") Long imageId, @Param("productId") Long productId);
}
