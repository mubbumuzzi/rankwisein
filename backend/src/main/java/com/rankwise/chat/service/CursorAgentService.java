package com.rankwise.chat.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rankwise.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class CursorAgentService {

    private static final Logger log = LoggerFactory.getLogger(CursorAgentService.class);
    private static final Set<String> TERMINAL_STATUSES = Set.of("FINISHED", "ERROR", "CANCELLED", "EXPIRED");

    private final AppProperties props;
    private final RestClient client;

    public CursorAgentService(AppProperties props) {
        this.props = props;
        this.client = RestClient.builder()
                .baseUrl("https://api.cursor.com")
                .build();
    }

    public boolean isConfigured() {
        AppProperties.Cursor cursor = props.getCursor();
        return cursor.isEnabled()
                && cursor.getApiKey() != null
                && !cursor.getApiKey().isBlank();
    }

    /**
     * Sends a prompt via Cursor Cloud Agents API. Creates a new agent when {@code existingAgentId}
     * is null; otherwise enqueues a follow-up run on the existing session agent.
     */
    public AgentCompletionResult complete(String existingAgentId, String promptText) {
        if (!isConfigured()) {
            throw new ChatException("AI counsellor is not configured. Please set CURSOR_API_KEY.");
        }
        if (promptText == null || promptText.isBlank()) {
            throw new ChatException("Empty prompt for AI service.");
        }

        AppProperties.Cursor cfg = props.getCursor();
        String agentId = existingAgentId;
        String runId;

        try {
            if (agentId == null || agentId.isBlank()) {
                CreateAgentResponse created = createAgent(promptText, cfg);
                agentId = created.agent.id;
                runId = created.run.id;
            } else {
                runId = createFollowUpRun(agentId, promptText);
            }

            RunStatus run = pollUntilTerminal(agentId, runId, cfg);
            if ("ERROR".equals(run.status)) {
                log.warn("Cursor agent run failed: agentId={} runId={}", agentId, runId);
                throw new ChatException("AI service could not complete your request. Please try again.");
            }
            if (!"FINISHED".equals(run.status)) {
                log.warn("Cursor agent run ended with status {}: agentId={} runId={}", run.status, agentId, runId);
                throw new ChatException("AI service timed out. Please try again.");
            }

            String text = run.result;
            if (text == null || text.isBlank()) {
                throw new ChatException("Empty response from AI service.");
            }
            return new AgentCompletionResult(text.trim(), cfg.getModel(), agentId, run.durationMs);
        } catch (ChatException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Cursor API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ChatException(mapCursorApiError(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChatException("AI service interrupted. Please try again.");
        } catch (Exception e) {
            log.warn("Cursor agent call failed", e);
            throw new ChatException("AI service temporarily unavailable. Please try again.");
        }
    }

    public void stream(String existingAgentId, String promptText, Consumer<String> onChunk, Runnable onComplete) {
        AgentCompletionResult result = complete(existingAgentId, promptText);
        onChunk.accept(result.content());
        onComplete.run();
    }

    public static String formatTurns(List<ChatTurn> turns) {
        StringBuilder sb = new StringBuilder();
        for (ChatTurn turn : turns) {
            sb.append(turn.role().toUpperCase()).append(":\n")
                    .append(turn.content().trim())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }

    public static String formatFollowUp(String contextBlock, String userMessage) {
        StringBuilder sb = new StringBuilder();
        if (contextBlock != null && !contextBlock.isBlank()) {
            sb.append("UPDATED CONTEXT:\n").append(contextBlock.trim()).append("\n\n");
        }
        sb.append("USER:\n").append(userMessage.trim());
        return sb.toString();
    }

    private CreateAgentResponse createAgent(String promptText, AppProperties.Cursor cfg) {
        Map<String, Object> body = Map.of(
                "prompt", Map.of("text", promptText),
                "model", Map.of("id", cfg.getModel()),
                "name", truncate(cfg.getAgentNamePrefix() + " chat", 100)
        );
        return client.post()
                .uri("/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> applyAuth(h, cfg.getApiKey()))
                .body(body)
                .retrieve()
                .body(CreateAgentResponse.class);
    }

    private String createFollowUpRun(String agentId, String promptText) {
        Map<String, Object> body = Map.of("prompt", Map.of("text", promptText));
        CreateRunResponse response = client.post()
                .uri("/v1/agents/{id}/runs", agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> applyAuth(h, props.getCursor().getApiKey()))
                .body(body)
                .retrieve()
                .body(CreateRunResponse.class);
        if (response == null || response.run == null || response.run.id == null) {
            throw new ChatException("Failed to start AI follow-up.");
        }
        return response.run.id;
    }

    private RunStatus pollUntilTerminal(String agentId, String runId, AppProperties.Cursor cfg)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + cfg.getMaxWaitMs();
        while (System.currentTimeMillis() < deadline) {
            RunStatus run = getRun(agentId, runId);
            if (run != null && TERMINAL_STATUSES.contains(run.status)) {
                return run;
            }
            Thread.sleep(cfg.getPollIntervalMs());
        }
        throw new ChatException("AI service timed out. Please try again.");
    }

    private RunStatus getRun(String agentId, String runId) {
        return client.get()
                .uri("/v1/agents/{id}/runs/{runId}", agentId, runId)
                .headers(h -> applyAuth(h, props.getCursor().getApiKey()))
                .retrieve()
                .body(RunStatus.class);
    }

    private static String mapCursorApiError(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body != null && body.contains("feature_unavailable")) {
            return "Cursor Cloud Agents require storage mode. In Cursor: Settings → Advanced → disable Local/Ghost (no-storage) mode, then retry.";
        }
        if (body != null && body.contains("\"message\"")) {
            int start = body.indexOf("\"message\"");
            int colon = body.indexOf(':', start);
            int quoteStart = body.indexOf('"', colon + 1);
            int quoteEnd = body.indexOf('"', quoteStart + 1);
            if (quoteStart >= 0 && quoteEnd > quoteStart) {
                return body.substring(quoteStart + 1, quoteEnd);
            }
        }
        return "AI service temporarily unavailable. Please try again.";
    }

    private static void applyAuth(org.springframework.http.HttpHeaders headers, String apiKey) {
        headers.setBearerAuth(apiKey.trim());
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    public record ChatTurn(String role, String content) {
    }

    public record AgentCompletionResult(
            String content,
            String model,
            String agentId,
            Long durationMs
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CreateAgentResponse {
        public AgentRef agent;
        public RunRef run;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CreateRunResponse {
        public RunRef run;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AgentRef {
        public String id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RunRef {
        public String id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RunStatus {
        public String id;
        public String status;
        public String result;
        public Long durationMs;
    }
}
