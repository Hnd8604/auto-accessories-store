package app.store.mapper;

import app.store.dto.request.PostCategoryRequest;
import app.store.dto.response.PostCategoryResponse;
import app.store.entity.PostCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PostCategoryMapper {

//    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "posts", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    PostCategory toPostCategory(PostCategoryRequest request);
    
    @Mapping(target = "postCount", expression = "java(postCategory.getPosts() != null ? (long) postCategory.getPosts().size() : 0L)")
    PostCategoryResponse toPostCategoryResponse(PostCategory postCategory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(PostCategoryRequest request, @MappingTarget PostCategory postCategory);
}
