package app.store.service;

import app.store.dto.request.PostRequest;
import app.store.dto.response.PostResponse;
import app.store.entity.Post;
import app.store.entity.PostCategory;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.PostMapper;
import app.store.repository.PostCategoryRepository;
import app.store.repository.PostRepository;
import app.store.repository.UserRepository;
import app.store.utils.SecurityUtils;
import app.store.utils.SlugUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {

    PostRepository postRepository;
    PostCategoryRepository postCategoryRepository;
    UserRepository userRepository;
    SlugUtil slugUtil;
    PostMapper postMapper;
    CloudinaryService CloudinaryService;
    @PreAuthorize("hasAuthority('POST_CREATE')")
    public PostResponse createPost(MultipartFile file, PostRequest request) {

        String authorId = SecurityUtils.currentUserId();
        // Tìm tác giả
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Tìm danh mục nếu có
        PostCategory postCategory = null;
        if (request.categoryId() != null) {
            postCategory = postCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.POST_CATEGORY_NOT_EXISTED));
        }

        Post post = postMapper.toPost(request);

        String thumbnailUrl = CloudinaryService.uploadImage(file, "store/posts");
        post.setThumbnailUrl(thumbnailUrl);

        // Tạo slug từ tiêu đề
        String baseSlug = slugUtil.toSlug(request.title());
        String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, postRepository::existsBySlug);
        post.setSlug(uniqueSlug);
        post.setAuthor(author);
        post.setViewCount(0L);
        post.setCategory(postCategory);

        return postMapper.toPostResponse(postRepository.save(post));
    }
    @PreAuthorize("hasAuthority('POST_UPDATE')")
    public PostResponse updatePost(MultipartFile file ,Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_EXISTED));

        // Cập nhật danh mục nếu có
        PostCategory category = null;
        if (request.categoryId() != null) {
            category = postCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.POST_CATEGORY_NOT_EXISTED));
        }


        // Cập nhật slug nếu tiêu đề thay đổi
        if (!post.getTitle().equals(request.title())) {
            String baseSlug = slugUtil.toSlug(request.title());
            String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, slug ->
                    !slug.equals(post.getSlug()) && postRepository.existsBySlug(slug));
            post.setSlug(uniqueSlug);
        }
        // Cập nhật thumbnail nếu có file mới
        if (file != null && !file.isEmpty()) {
            String thumbnailUrl = CloudinaryService.uploadImage(file, "store/posts");
            post.setThumbnailUrl(thumbnailUrl);
        }

        post.setTitle(request.title());
        post.setShortDescription(request.shortDescription());
        post.setContent(request.content());
        post.setPublished(request.published());
        post.setCategory(category);


        return postMapper.toPostResponse(postRepository.save(post));
    }
    @PreAuthorize("hasAuthority('POST_DELETE')")
    public void deletePost(Long id) {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_EXISTED));

        postRepository.delete(post);

    }
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_EXISTED));

        return postMapper.toPostResponse(post);
    }
    @Transactional(readOnly = true)
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_EXISTED));

        return postMapper.toPostResponse(post);
    }
    @Transactional
    public PostResponse getPostBySlugAndIncrementView(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_EXISTED));

        // Chỉ tăng view count cho bài viết đã xuất bản
        if (post.getPublished()) {
            postRepository.incrementViewCount(post.getId());
            post.setViewCount(post.getViewCount() + 1);
        }

        return postMapper.toPostResponse(post);
    }
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable).map(postMapper::toPostResponse);
    }
    @Transactional(readOnly = true)
    public Page<PostResponse> getPublishedPosts(Pageable pageable) {

        return postRepository.findPublishedPosts(pageable).map(postMapper::toPostResponse);
    }
    @Transactional(readOnly = true)
    public Page<PostResponse> searchPublishedPosts(String keyword, Pageable pageable) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return postRepository.findPublishedPosts(pageable).map(postMapper::toPostResponse);
        }

        return postRepository.findPublishedPostsByKeyword(keyword.trim(), pageable)
                .map(postMapper::toPostResponse);
    }
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByCategory(Long categoryId, Pageable pageable) {
        PostCategory category = postCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_CATEGORY_NOT_EXISTED));

        return postRepository.findPublishedPostsByCategory(category, pageable)
                .map(postMapper::toPostResponse);
    }
    @Transactional(readOnly = true)
    public Page<PostResponse> getRelatedPosts(Long postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_EXISTED));

        if (post.getCategory() == null) {
            return Page.empty();
        }

        return postRepository.findRelatedPosts(post.getCategory(), postId,pageable)
                .map(postMapper::toPostResponse);
    }
    @Transactional(readOnly = true)
    public Page<PostResponse> getMostViewedPosts(Pageable pageable) {
        return postRepository.findMostViewedPosts(pageable)
                .map(postMapper::toPostResponse);
    }
    @PreAuthorize("hasAuthority('POST_TOGGLE_PUBLISH')")
    public void togglePublishStatus(Long id) {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_EXISTED));

        post.setPublished(!post.getPublished());
        postRepository.save(post);

    }
}
