package app.store.repository;

import app.store.entity.ProfessionalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ProfessionalService, Long> {

    boolean existsBySlug(String slug);

    Optional<ProfessionalService> findBySlug(String slug);

    @Query("SELECT s FROM ProfessionalService s LEFT JOIN FETCH s.images ORDER BY s.displayOrder ASC NULLS LAST, s.createdAt ASC")
    List<ProfessionalService> findAllWithImages();
}
