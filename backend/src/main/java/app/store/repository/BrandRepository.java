package app.store.repository;

import app.store.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
    List<Brand> findByCategoriesId(Long categoryId);
    boolean existsByIdAndCategoriesId(Long brandId, Long categoryId);
}
