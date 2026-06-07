package com.rankwise.chat.service;

import com.rankwise.chat.dto.*;
import com.rankwise.chat.entity.ChatMessage;
import com.rankwise.chat.entity.ChatSession;
import com.rankwise.chat.entity.StudentProfile;
import com.rankwise.chat.repository.*;
import com.rankwise.common.csv.CsvExportService;
import com.rankwise.common.dto.PageResponse;
import com.rankwise.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChatAdminService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatUserRepository chatUserRepository;
    private final StudentProfileRepository profileRepository;
    private final ChatAnalyticsEventRepository analyticsRepository;
    private final ChatSessionService sessionService;
    private final CsvExportService csvExportService;

    public ChatAdminService(ChatSessionRepository sessionRepository,
                            ChatMessageRepository messageRepository,
                            ChatUserRepository chatUserRepository,
                            StudentProfileRepository profileRepository,
                            ChatAnalyticsEventRepository analyticsRepository,
                            ChatSessionService sessionService,
                            CsvExportService csvExportService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chatUserRepository = chatUserRepository;
        this.profileRepository = profileRepository;
        this.analyticsRepository = analyticsRepository;
        this.sessionService = sessionService;
        this.csvExportService = csvExportService;
    }

    @Transactional(readOnly = true)
    public ChatAdminStatsResponse stats() {
        long sessions = sessionRepository.count();
        long messages = messageRepository.count();
        long users = chatUserRepository.count();
        long profiles = profileRepository.count();

        List<Map<String, Object>> topEvents = analyticsRepository.countGroupedByEventType().stream()
                .map(row -> Map.<String, Object>of("eventType", row[0], "count", row[1]))
                .toList();

        List<Map<String, Object>> recentProfiles = profileRepository.findAll().stream()
                .sorted(Comparator.comparing(StudentProfile::getUpdatedAt).reversed())
                .limit(10)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("rank", p.getRank());
                    m.put("category", p.getCategory());
                    m.put("gender", p.getGender());
                    m.put("branches", p.getPreferredBranches());
                    m.put("updatedAt", p.getUpdatedAt().format(ISO));
                    return m;
                })
                .toList();

        return new ChatAdminStatsResponse(
                sessions,
                messages,
                users,
                profiles,
                countEvent(ChatAnalyticsService.EVENT_CHAT_OPEN),
                countEvent(ChatAnalyticsService.EVENT_MESSAGE_SENT),
                countEvent(ChatAnalyticsService.EVENT_WHATSAPP_COMMUNITY),
                countEvent(ChatAnalyticsService.EVENT_PREDICTOR_USED),
                countEvent(ChatAnalyticsService.EVENT_COMPARISON_USED),
                topEvents,
                recentProfiles
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatSessionSummaryResponse> listSessions(String q, Pageable pageable) {
        Page<ChatSession> page = sessionRepository.search(q, pageable);
        List<ChatSessionSummaryResponse> content = page.getContent().stream().map(s -> {
            StudentProfile profile = profileRepository.findByChatUserId(s.getChatUserId()).orElse(null);
            return new ChatSessionSummaryResponse(
                    s.getId(),
                    s.getChatUserId(),
                    s.getTitle(),
                    s.getMessageCount(),
                    s.isLeadCtaShown(),
                    s.getCreatedAt().format(ISO),
                    s.getUpdatedAt().format(ISO),
                    profile != null ? profile.getRank() : null,
                    profile != null ? profile.getCategory() : null
            );
        }).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public ChatSessionDetailResponse sessionDetail(Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        StudentProfile profile = profileRepository.findByChatUserId(session.getChatUserId()).orElse(null);
        List<ChatMessageResponse> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(sessionService::toMessageResponse)
                .toList();
        return new ChatSessionDetailResponse(
                session.getId(),
                session.getChatUserId(),
                session.getTitle(),
                sessionService.toProfileResponse(profile),
                messages
        );
    }

    @Transactional(readOnly = true)
    public String exportSessionsCsv() {
        List<ChatSessionSummaryResponse> all = listSessions("", Pageable.ofSize(10_000)).getContent();
        return csvExportService.toCsv(
                List.of("SessionId", "UserId", "Title", "Messages", "Rank", "Category", "Created", "Updated"),
                all,
                s -> List.of(
                        String.valueOf(s.id()),
                        String.valueOf(s.chatUserId()),
                        s.title() != null ? s.title() : "",
                        String.valueOf(s.messageCount()),
                        s.profileRank() != null ? String.valueOf(s.profileRank()) : "",
                        s.profileCategory() != null ? s.profileCategory() : "",
                        s.createdAt(),
                        s.updatedAt()
                )
        );
    }

    private long countEvent(String type) {
        return analyticsRepository.countByEventType(type);
    }
}
