package app.store.repository;

import app.store.entity.Post;
import app.store.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    Optional<Post> findBySlug(String slug);
    
    @Query("SELECT p FROM Post p WHERE p.published = true")
    Page<Post> findPublishedPosts(Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.published = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> findPublishedPostsByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.category = :category AND p.published = true")
    Page<Post> findPublishedPostsByCategory(@Param("category") PostCategory category, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.category = :category AND p.published = true AND p.id != :excludeId")
    Page<Post> findRelatedPosts(@Param("category") PostCategory category, @Param("excludeId") Long excludeId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.published = true")
    Page<Post> findMostViewedPosts(Pageable pageable);
    
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);
    
    boolean existsBySlug(String slug);
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.published = true")
    Long countPublishedPosts();
}
