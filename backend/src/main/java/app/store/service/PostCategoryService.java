package app.store.service;

import app.store.dto.request.PostCategoryRequest;
import app.store.dto.response.PostCategoryResponse;
import app.store.entity.PostCategory;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.PostCategoryMapper;
import app.store.repository.PostCategoryRepository;
import app.store.utils.SlugUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostCategoryService {
    
    PostCategoryRepository postCategoryRepository;
    SlugUtil slugUtil;
    PostCategoryMapper postCategoryMapper;
    @PreAuthorize("hasAuthority('POST_CATEGORY_CREATE')")
    public PostCategoryResponse createCategory(PostCategoryRequest request) {

        // Kiểm tra tên danh mục đã tồn tại chưa
        if (postCategoryRepository.existsByName(request.name())) {
            throw new AppException(ErrorCode.POST_CATEGORY_EXISTED);
        }
        PostCategory postCategory = postCategoryMapper.toPostCategory(request);

        // Tạo slug từ tên danh mục
        String baseSlug = slugUtil.toSlug(request.name());
        String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, postCategoryRepository::existsBySlug);

        postCategory.setSlug(uniqueSlug);

        return postCategoryMapper.toPostCategoryResponse(postCategoryRepository.save(postCategory));
    }
    @PreAuthorize("hasAuthority('POST_CATEGORY_UPDATE')")
    public PostCategoryResponse updateCategory(Long id, PostCategoryRequest request) {

        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_CATEGORY_NOT_EXISTED));
        
        // Kiểm tra tên danh mục mới có trùng với danh mục khác không
        if (!category.getName().equals(request.name()) && 
            postCategoryRepository.existsByName(request.name())) {
            throw new AppException(ErrorCode.POST_CATEGORY_EXISTED);
        }
        
        // Cập nhật slug nếu tên thay đổi
        if (!category.getName().equals(request.name())) {
            String baseSlug = slugUtil.toSlug(request.name());
            String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, slug -> 
                !slug.equals(category.getSlug()) && postCategoryRepository.existsBySlug(slug));
            category.setSlug(uniqueSlug);
        }
        
        category.setName(request.name());
        category.setDescription(request.description());


        return postCategoryMapper.toPostCategoryResponse(postCategoryRepository.save(category));
    }
    @PreAuthorize("hasAuthority('POST_CATEGORY_DELETE')")
    public void deleteCategory(Long id) {
        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_CATEGORY_NOT_EXISTED));
        
        // Kiểm tra xem danh mục có bài viết nào không
        if (category.getPosts() != null && !category.getPosts().isEmpty()) {
            throw new AppException(ErrorCode.POST_CATEGORY_HAS_POSTS);
        }
        
        postCategoryRepository.delete(category);
    }
    @Transactional(readOnly = true)
    public PostCategoryResponse getCategoryById(Long id) {
        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_CATEGORY_NOT_EXISTED));
        
        return postCategoryMapper.toPostCategoryResponse(category);
    }
    @Transactional(readOnly = true)
    public PostCategoryResponse getCategoryBySlug(String slug) {
        PostCategory category = postCategoryRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.POST_CATEGORY_NOT_EXISTED));

        return postCategoryMapper.toPostCategoryResponse(category);
    }
    @Transactional(readOnly = true)
    public List<PostCategoryResponse> getAllCategories() {
        return postCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(postCategoryMapper::toPostCategoryResponse)
                .toList();
    }
    @Transactional(readOnly = true)
//    @PreAuthorize("hasAuthority('POST_CATEGORY_SEARCH')")
    public List<PostCategoryResponse> searchCategories(String keyword) {

        List<PostCategory> postCategoryList;
        if (keyword == null || keyword.trim().isEmpty()) {
            postCategoryList = postCategoryRepository.findAll();
        } else {
            postCategoryList = postCategoryRepository.findByKeyword(keyword.trim());
        }
        
        return postCategoryList
                .stream()
                .map(postCategoryMapper::toPostCategoryResponse)
                .toList();
    }

}
