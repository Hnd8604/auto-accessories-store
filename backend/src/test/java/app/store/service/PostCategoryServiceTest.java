package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import app.store.dto.request.PostCategoryRequest;
import app.store.dto.response.PostCategoryResponse;
import app.store.entity.Post;
import app.store.entity.PostCategory;
import app.store.mapper.PostCategoryMapper;
import app.store.repository.PostCategoryRepository;
import app.store.utils.SlugUtil;

@ExtendWith(MockitoExtension.class)
public class PostCategoryServiceTest {

    @Mock
    PostCategoryRepository postCategoryRepository;
    @Mock
    SlugUtil slugUtil;
    @Mock
    PostCategoryMapper postCategoryMapper;
    @InjectMocks
    PostCategoryService postCategoryService;

    private PostCategoryRequest request(String name, String description) {
        PostCategoryRequest request = new PostCategoryRequest(); // DTO này không có @Builder
        request.setName(name);
        request.setDescription(description);
        return request;
    }

    private PostCategory buildCategory(Long id, String name, String slug) {
        PostCategory category = new PostCategory();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        return category;
    }

    // ==================== create ====================

    @Test
    void createCategory_shouldGenerateUniqueSlug() {
        PostCategoryRequest request = request("Tin tức xe hơi", "mô tả");
        PostCategory mapped = new PostCategory();

        when(postCategoryRepository.existsByName("Tin tức xe hơi")).thenReturn(false);
        when(postCategoryMapper.toPostCategory(request)).thenReturn(mapped);
        when(slugUtil.toSlug("Tin tức xe hơi")).thenReturn("tin-tuc-xe-hoi");
        when(slugUtil.createUniqueSlug(eq("tin-tuc-xe-hoi"), any())).thenReturn("tin-tuc-xe-hoi-1");
        when(postCategoryRepository.save(mapped)).thenReturn(mapped);
        when(postCategoryMapper.toPostCategoryResponse(mapped)).thenReturn(new PostCategoryResponse());

        postCategoryService.createCategory(request);

        assertThat(mapped.getSlug()).isEqualTo("tin-tuc-xe-hoi-1");
    }

    @Test
    void createCategory_shouldThrow_whenNameDuplicated() {
        PostCategoryRequest request = request("Tin tức", null);

        when(postCategoryRepository.existsByName("Tin tức")).thenReturn(true);

        assertThatThrownBy(() -> postCategoryService.createCategory(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã tồn tại");

        verify(postCategoryRepository, never()).save(any());
    }

    // ==================== update ====================

    @Test
    void updateCategory_shouldRegenerateSlug_whenNameChanged() {
        PostCategory category = buildCategory(1L, "Tên cũ", "ten-cu");
        PostCategoryRequest request = request("Tên mới", "mô tả mới");

        when(postCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postCategoryRepository.existsByName("Tên mới")).thenReturn(false);
        when(slugUtil.toSlug("Tên mới")).thenReturn("ten-moi");
        when(slugUtil.createUniqueSlug(eq("ten-moi"), any())).thenReturn("ten-moi");
        when(postCategoryRepository.save(category)).thenReturn(category);
        when(postCategoryMapper.toPostCategoryResponse(category)).thenReturn(new PostCategoryResponse());

        postCategoryService.updateCategory(1L, request);

        assertThat(category.getSlug()).isEqualTo("ten-moi");
        assertThat(category.getName()).isEqualTo("Tên mới");
        assertThat(category.getDescription()).isEqualTo("mô tả mới");
    }

    @Test
    void updateCategory_shouldKeepSlug_whenNameUnchanged() {
        PostCategory category = buildCategory(1L, "Tin tức", "tin-tuc");
        PostCategoryRequest request = request("Tin tức", "mô tả mới");

        when(postCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postCategoryRepository.save(category)).thenReturn(category);
        when(postCategoryMapper.toPostCategoryResponse(category)).thenReturn(new PostCategoryResponse());

        postCategoryService.updateCategory(1L, request);

        assertThat(category.getSlug()).isEqualTo("tin-tuc");
        verify(slugUtil, never()).toSlug(any());
    }

    @Test
    void updateCategory_shouldThrow_whenNewNameBelongsToAnotherCategory() {
        PostCategory category = buildCategory(1L, "Tên cũ", "ten-cu");
        PostCategoryRequest request = request("Tên đã có", null);

        when(postCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postCategoryRepository.existsByName("Tên đã có")).thenReturn(true);

        assertThatThrownBy(() -> postCategoryService.updateCategory(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    void updateCategory_shouldThrow_whenNotFound() {
        when(postCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCategoryService.updateCategory(99L, request("x", null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy danh mục");
    }

    // ==================== delete ====================

    @Test
    void deleteCategory_shouldDelete_whenNoPosts() {
        PostCategory category = buildCategory(1L, "Tin tức", "tin-tuc");
        category.setPosts(List.of());

        when(postCategoryRepository.findById(1L)).thenReturn(Optional.of(category));

        postCategoryService.deleteCategory(1L);

        verify(postCategoryRepository).delete(category);
    }

    @Test
    void deleteCategory_shouldThrow_whenCategoryStillHasPosts() {
        PostCategory category = buildCategory(1L, "Tin tức", "tin-tuc");
        category.setPosts(List.of(new Post()));

        when(postCategoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> postCategoryService.deleteCategory(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã có bài viết");

        verify(postCategoryRepository, never()).delete(any());
    }

    // ==================== read / search ====================

    @Test
    void getCategoryBySlug_shouldThrow_whenNotFound() {
        when(postCategoryRepository.findBySlug("khong-ton-tai")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCategoryService.getCategoryBySlug("khong-ton-tai"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("slug");
    }

    @Test
    void getAllCategories_shouldSortByNameAsc() {
        PostCategory category = buildCategory(1L, "Tin tức", "tin-tuc");

        when(postCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(category));
        when(postCategoryMapper.toPostCategoryResponse(category)).thenReturn(new PostCategoryResponse());

        assertThat(postCategoryService.getAllCategories()).hasSize(1);
    }

    @Test
    void searchCategories_shouldReturnAll_whenKeywordBlank() {
        when(postCategoryRepository.findAll()).thenReturn(List.of());

        assertThat(postCategoryService.searchCategories("   ")).isEmpty();

        verify(postCategoryRepository, never()).findByKeyword(any());
    }

    @Test
    void searchCategories_shouldTrimKeyword_beforeQuerying() {
        PostCategory category = buildCategory(1L, "Tin tức", "tin-tuc");

        when(postCategoryRepository.findByKeyword("tin")).thenReturn(List.of(category));
        when(postCategoryMapper.toPostCategoryResponse(category)).thenReturn(new PostCategoryResponse());

        assertThat(postCategoryService.searchCategories("  tin  ")).hasSize(1);
    }
}
