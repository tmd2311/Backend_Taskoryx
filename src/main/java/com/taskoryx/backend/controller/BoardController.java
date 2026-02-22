package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.board.*;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.board.BoardColumnResponse;
import com.taskoryx.backend.dto.response.board.BoardResponse;
import com.taskoryx.backend.dto.response.board.KanbanBoardResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.BoardService;
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

/**
 * REST Controller cho Board (Kanban)
 *
 * GET    /api/projects/{projectId}/boards          - Danh sách boards
 * POST   /api/projects/{projectId}/boards          - Tạo board mới
 * GET    /api/boards/{id}/kanban                   - Lấy Kanban view (full board)
 * PUT    /api/boards/{id}                          - Cập nhật board
 * DELETE /api/boards/{id}                          - Xóa board
 * POST   /api/boards/{id}/columns                  - Thêm cột
 * PUT    /api/columns/{id}                         - Cập nhật cột
 * PATCH  /api/columns/{id}/move                    - Di chuyển cột (drag & drop)
 * DELETE /api/columns/{id}                         - Xóa cột
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Boards", description = "Quản lý Kanban board và cột")
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/projects/{projectId}/boards")
    @Operation(summary = "Lấy danh sách boards trong project")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoards(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                boardService.getBoardsByProject(projectId, principal)));
    }

    @PostMapping("/projects/{projectId}/boards")
    @Operation(summary = "Tạo board mới trong project")
    public ResponseEntity<ApiResponse<BoardResponse>> createBoard(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBoardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo board thành công",
                        boardService.createBoard(projectId, request, principal)));
    }

    @GetMapping("/boards/{id}/kanban")
    @Operation(summary = "Lấy Kanban board view đầy đủ (dùng cho drag & drop)")
    public ResponseEntity<ApiResponse<KanbanBoardResponse>> getKanbanBoard(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getKanbanBoard(id, principal)));
    }

    @PutMapping("/boards/{id}")
    @Operation(summary = "Cập nhật thông tin board")
    public ResponseEntity<ApiResponse<BoardResponse>> updateBoard(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateBoardRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật board thành công",
                boardService.updateBoard(id, request, principal)));
    }

    @DeleteMapping("/boards/{id}")
    @Operation(summary = "Xóa board")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boardService.deleteBoard(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa board thành công"));
    }

    // ========== COLUMNS ==========

    @PostMapping("/boards/{boardId}/columns")
    @Operation(summary = "Thêm cột mới vào board")
    public ResponseEntity<ApiResponse<BoardColumnResponse>> createColumn(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateColumnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo cột thành công",
                        boardService.createColumn(boardId, request, principal)));
    }

    @PutMapping("/columns/{id}")
    @Operation(summary = "Cập nhật thông tin cột")
    public ResponseEntity<ApiResponse<BoardColumnResponse>> updateColumn(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateColumnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cột thành công",
                boardService.updateColumn(id, request, principal)));
    }

    @PatchMapping("/columns/{id}/move")
    @Operation(summary = "Di chuyển cột (thay đổi thứ tự - drag & drop)")
    public ResponseEntity<ApiResponse<Void>> moveColumn(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MoveColumnRequest request) {
        boardService.moveColumn(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Di chuyển cột thành công"));
    }

    @DeleteMapping("/columns/{id}")
    @Operation(summary = "Xóa cột")
    public ResponseEntity<ApiResponse<Void>> deleteColumn(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boardService.deleteColumn(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa cột thành công"));
    }
}
