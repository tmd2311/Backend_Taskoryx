package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.category.CreateCategoryRequest;
import com.taskoryx.backend.dto.request.category.UpdateCategoryRequest;
import com.taskoryx.backend.dto.response.category.CategoryResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.IssueCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Issue Categories", description = "Quản lý danh mục task của project")
public class IssueCategoryController {

    private final IssueCategoryService categoryService;

    @PostMapping("/projects/{projectId}/categories")
    @Operation(summary = "Tạo danh mục mới")
    public ResponseEntity<CategoryResponse> createCategory(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(projectId, request, principal));
    }

    @GetMapping("/projects/{projectId}/categories")
    @Operation(summary = "Lấy danh sách danh mục của project")
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.getCategories(projectId, principal));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Cập nhật danh mục")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request, principal));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Xóa danh mục")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        categoryService.deleteCategory(id, principal);
        return ResponseEntity.noContent().build();
    }
}
