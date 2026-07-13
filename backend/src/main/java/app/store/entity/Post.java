package app.store.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "posts")
public class Post extends BaseEntityLong {
    
    @Column(nullable = false, length = 500)
    String title;
    
    @Column(unique = true, nullable = false, length = 500)
    String slug;
    
    @Column(length = 1000)
    String shortDescription;
    
    String thumbnailUrl;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    String content;
    
    @Column(nullable = false)
    @Builder.Default
    Boolean published = false;
    
    @Column
    @Builder.Default
    Long viewCount = 0L;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    PostCategory category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    User author;
}
