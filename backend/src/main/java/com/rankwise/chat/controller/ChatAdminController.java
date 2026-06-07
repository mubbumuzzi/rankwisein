package com.rankwise.chat.controller;

import com.rankwise.chat.dto.*;
import com.rankwise.chat.service.ChatAdminService;
import com.rankwise.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chat")
@Tag(name = "Chat Admin", description = "AI counsellor analytics and conversations")
public class ChatAdminController {

    private final ChatAdminService adminService;

    public ChatAdminController(ChatAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Chat analytics dashboard stats")
    public ChatAdminStatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/sessions")
    @Operation(summary = "Search chat sessions")
    public PageResponse<ChatSessionSummaryResponse> sessions(@RequestParam(required = false) String q,
                                                             Pageable pageable) {
        return adminService.listSessions(q, pageable);
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get session with full message history")
    public ChatSessionDetailResponse sessionDetail(@PathVariable Long sessionId) {
        return adminService.sessionDetail(sessionId);
    }

    @GetMapping("/export")
    @Operation(summary = "Export chat sessions CSV")
    public ResponseEntity<String> export() {
        String csv = adminService.exportSessionsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=chat-sessions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
