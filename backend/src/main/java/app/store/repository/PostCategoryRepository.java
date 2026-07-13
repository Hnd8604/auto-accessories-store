package app.store.repository;

import app.store.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {
    
    Optional<PostCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
    @Query("SELECT pc FROM PostCategory pc WHERE " +
           "LOWER(pc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(pc.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PostCategory> findByKeyword(@Param("keyword") String keyword);
    


//    @Query("SELECT COUNT(p) FROM Post p WHERE p.category.id = :categoryId")
//    long countPostByCategory(@Param("categoryId") Long categoryId);


}
