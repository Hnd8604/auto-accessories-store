package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import app.store.dto.request.PostRequest;
import app.store.dto.response.PostResponse;
import app.store.entity.Post;
import app.store.entity.PostCategory;
import app.store.entity.User;
import app.store.mapper.PostMapper;
import app.store.repository.PostCategoryRepository;
import app.store.repository.PostRepository;
import app.store.repository.UserRepository;
import app.store.utils.SlugUtil;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    @Mock
    PostRepository postRepository;
    @Mock
    PostCategoryRepository postCategoryRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    SlugUtil slugUtil;
    @Mock
    PostMapper postMapper;
    @Mock
    CloudinaryService cloudinaryService;
    @InjectMocks
    PostService postService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockMultipartFile emptyFile() {
        return new MockMultipartFile("file", new byte[0]);
    }

    @Test
    void createPost_happyPath_setsSlugAuthorAndCategory() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", null));

        PostRequest request = new PostRequest();
        request.setTitle("Đèn LED");
        request.setContent("nội dung");
        request.setPublished(true);
        request.setCategoryId(1L);

        User author = new User();
        author.setUsername("john");

        PostCategory category = new PostCategory();
        category.setId(1L);

        Post mappedPost = new Post();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(author));
        when(postCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postMapper.toPost(request)).thenReturn(mappedPost);
        when(cloudinaryService.uploadImage(any(), any())).thenReturn("http://img.url/x.png");
        when(slugUtil.toSlug("Đèn LED")).thenReturn("den-led");
        when(slugUtil.createUniqueSlug(any(), any())).thenReturn("den-led");
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toPostResponse(any(Post.class))).thenReturn(new PostResponse());

        postService.createPost(emptyFile(), request);

        assertThat(mappedPost.getSlug()).isEqualTo("den-led");
        assertThat(mappedPost.getAuthor()).isEqualTo(author);
        assertThat(mappedPost.getCategory()).isEqualTo(category);
        assertThat(mappedPost.getViewCount()).isEqualTo(0L);
        assertThat(mappedPost.getThumbnailUrl()).isEqualTo("http://img.url/x.png");
    }

    @Test
    void createPost_shouldThrow_whenCategoryNotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", null));

        PostRequest request = new PostRequest();
        request.setTitle("Đèn LED");
        request.setCategoryId(99L);

        User author = new User();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(author));
        when(postCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(emptyFile(), request))
                .isInstanceOf(RuntimeException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_shouldRegenerateSlug_whenTitleChanged() {
        Post existing = new Post();
        existing.setId(1L);
        existing.setTitle("Tiêu đề cũ");
        existing.setSlug("tieu-de-cu");

        PostRequest request = new PostRequest();
        request.setTitle("Tiêu đề mới");
        request.setContent("nội dung mới");
        request.setPublished(true);

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(slugUtil.toSlug("Tiêu đề mới")).thenReturn("tieu-de-moi");
        when(slugUtil.createUniqueSlug(any(), any())).thenReturn("tieu-de-moi");
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toPostResponse(any(Post.class))).thenReturn(new PostResponse());

        postService.updatePost(null, 1L, request);

        assertThat(existing.getSlug()).isEqualTo("tieu-de-moi");
        assertThat(existing.getTitle()).isEqualTo("Tiêu đề mới");
    }

    @Test
    void updatePost_shouldKeepSlug_whenTitleUnchanged() {
        Post existing = new Post();
        existing.setId(1L);
        existing.setTitle("Tiêu đề");
        existing.setSlug("tieu-de");

        PostRequest request = new PostRequest();
        request.setTitle("Tiêu đề");
        request.setContent("nội dung mới");
        request.setPublished(true);

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toPostResponse(any(Post.class))).thenReturn(new PostResponse());

        postService.updatePost(null, 1L, request);

        assertThat(existing.getSlug()).isEqualTo("tieu-de");
        verify(slugUtil, never()).toSlug(any());
    }

    @Test
    void updatePost_shouldThrow_whenNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        PostRequest request = new PostRequest();
        request.setTitle("x");

        assertThatThrownBy(() -> postService.updatePost(null, 99L, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getPostById_shouldThrow_whenNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getPostBySlugAndIncrementView_shouldIncrement_whenPublished() {
        Post post = new Post();
        post.setId(1L);
        post.setSlug("den-led");
        post.setPublished(true);
        post.setViewCount(5L);

        when(postRepository.findBySlug("den-led")).thenReturn(Optional.of(post));
        when(postMapper.toPostResponse(post)).thenReturn(new PostResponse());

        postService.getPostBySlugAndIncrementView("den-led");

        verify(postRepository).incrementViewCount(1L);
        assertThat(post.getViewCount()).isEqualTo(6L);
    }

    @Test
    void getPostBySlugAndIncrementView_shouldNotIncrement_whenNotPublished() {
        Post post = new Post();
        post.setId(1L);
        post.setSlug("den-led");
        post.setPublished(false);
        post.setViewCount(5L);

        when(postRepository.findBySlug("den-led")).thenReturn(Optional.of(post));
        when(postMapper.toPostResponse(post)).thenReturn(new PostResponse());

        postService.getPostBySlugAndIncrementView("den-led");

        verify(postRepository, never()).incrementViewCount(any());
        assertThat(post.getViewCount()).isEqualTo(5L);
    }

    @Test
    void togglePublishStatus_shouldFlipPublishedFlag() {
        Post post = new Post();
        post.setId(1L);
        post.setPublished(false);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.togglePublishStatus(1L);

        assertThat(post.getPublished()).isTrue();
        verify(postRepository).save(post);
    }

    @Test
    void getRelatedPosts_shouldReturnEmptyPage_whenPostHasNoCategory() {
        Post post = new Post();
        post.setId(1L);
        post.setCategory(null);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Page<PostResponse> result = postService.getRelatedPosts(1L, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
        verify(postRepository, never()).findRelatedPosts(any(), any(), any());
    }
}
