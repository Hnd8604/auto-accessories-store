package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.PostRequest;
import app.store.dto.request.ProductSearchRequest;
import app.store.dto.response.auth.ApiResponse;
import app.store.dto.response.PostResponse;
import app.store.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static app.store.utils.SortUtils.buildSort;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/posts")
@Tag(name = "Post Management", description = "APIs for managing blog posts including CRUD operations, publishing, and public access")
public class PostController {
    
    PostService postService;

    @PostMapping
    @Operation(
            summary = "Create a new post",
            description = "Creates a new blog post. Only accessible by admin users."
    )
    public ApiResponse<PostResponse> createPost(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("post") PostRequest request) {
        PostResponse response = postService.createPost(file, request);
        return ApiResponse.<PostResponse>builder()
                .message(ResponseMessage.CREATE_POST_SUCCESS)
                .result(response)
                .build();
    }

    @PutMapping("/{postId}")
    @Operation(
            summary = "Update post",
            description = "Updates an existing blog post by ID."
    )
    public ApiResponse<PostResponse> updatePost(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @PathVariable Long postId,
            @Valid @RequestPart("post") PostRequest request) {
        PostResponse response = postService.updatePost(file, postId, request);
        return ApiResponse.<PostResponse>builder()
                .message(ResponseMessage.UPDATE_POST_SUCCESS)
                .result(response)
                .build();
    }
    @DeleteMapping("/{postId}")
    @Operation(
        summary = "Delete post",
        description = "Permanently deletes a blog post by ID. Only accessible by admin users."
    )
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.DELETE_POST_SUCCESS)
                .build();
    }
    
    @GetMapping("/id/{postId}")
    @Operation(
        summary = "Get post by ID",
        description = "Retrieves detailed information of a post by ID including draft posts. Only accessible by admin users."
    )
    public ApiResponse<PostResponse> getPostById(@PathVariable Long postId) {
        PostResponse response = postService.getPostById(postId);
        return ApiResponse.<PostResponse>builder()
                .message(ResponseMessage.GET_POST_SUCCESS)
                .result(response)
                .build();
    }
    
    @GetMapping
    @Operation(
        summary = "Get all posts",
        description = "Retrieves all posts with pagination including drafts. Only accessible by admin users. Sorted by creation date descending."
    )
    public ApiResponse<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<PostResponse> response = postService.getAllPosts(pageable);
        return ApiResponse.<Page<PostResponse>>builder()
                .message(ResponseMessage.GET_ALL_POSTS_SUCCESS)
                .result(response)
                .build();
    }
    
    @PatchMapping("/{postId}/toggle-publish")
    @Operation(
        summary = "Toggle publish status",
        description = "Toggles the publish status of a post (publish/unpublish). Only accessible by admin users."
    )
    public ApiResponse<Void> togglePublishStatus(@PathVariable Long postId) {
        postService.togglePublishStatus(postId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.TOGGLE_POST_PUBLISH_SUCCESS)
                .build();
    }

    @GetMapping("/published")
    @Operation(
        summary = "Get published posts",
        description = "Retrieves all published posts with pagination. Accessible by authenticated users. Only returns posts with published status."
    )
    public ApiResponse<Page<PostResponse>> getPublishedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<PostResponse> response = postService.getPublishedPosts(pageable);
        return ApiResponse.<Page<PostResponse>>builder()
                .message(ResponseMessage.GET_ALL_POSTS_SUCCESS)
                .result(response)
                .build();
    }
    
    @GetMapping("/search")
    @Operation(
        summary = "Search published posts",
        description = "Searches published posts by keyword with pagination. Accessible by authenticated users. Searches in title and short description fields only."
    )
    public ApiResponse<Page<PostResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<PostResponse> response = postService.searchPublishedPosts(keyword, pageable);
        return ApiResponse.<Page<PostResponse>>builder()
                .message(ResponseMessage.SEARCH_POSTS_SUCCESS)
                .result(response)
                .build();
    }
    
    @GetMapping("/slug/{slug}")
    @Operation(
        summary = "Get post by slug",
        description = "Retrieves post details by slug and automatically increments view count. Accessible by authenticated users. Slug is SEO-friendly URL identifier."
    )
    public ApiResponse<PostResponse> getPostBySlug(@PathVariable String slug) {
        PostResponse response = postService.getPostBySlugAndIncrementView(slug);
        return ApiResponse.<PostResponse>builder()
                .message(ResponseMessage.GET_POST_SUCCESS)
                .result(response)
                .build();
    }
    
    @GetMapping("/category/{categoryId}")
    @Operation(
        summary = "Get posts by category",
        description = "Retrieves published posts filtered by category with pagination. Accessible by authenticated users."
    )
    public ApiResponse<Page<PostResponse>> getPostsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<PostResponse> response = postService.getPostsByCategory(categoryId,pageable);
        return ApiResponse.<Page<PostResponse>>builder()
                .message(ResponseMessage.GET_POSTS_BY_CATEGORY_SUCCESS)
                .result(response)
                .build();
    }
    
    @GetMapping("/{postId}/related")
    @Operation(
        summary = "Get related posts",
        description = "Retrieves related posts from the same category. Accessible by authenticated users. Returns posts sorted by creation date."
    )
    public ApiResponse<Page<PostResponse>> getRelatedPosts(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<PostResponse> response = postService.getRelatedPosts(postId, pageable);
        return ApiResponse.<Page<PostResponse>>builder()
                .message(ResponseMessage.GET_RELATED_POSTS_SUCCESS)
                .result(response)
                .build();
    }
    
    @GetMapping("/most-viewed")
    @Operation(
        summary = "Get most viewed posts",
        description = "Retrieves the most viewed published posts. Accessible by authenticated users. Sorted by view count descending."
    )
    public ApiResponse<Page<PostResponse>> getMostViewedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "viewCount,DESC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<PostResponse> response = postService.getMostViewedPosts(pageable);
        return ApiResponse.<Page<PostResponse>>builder()
                .message(ResponseMessage.GET_MOST_VIEWED_POSTS_SUCCESS)
                .result(response)
                .build();
    }
}
