package com.rankwise.chat.service;

import com.rankwise.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ChatSafetyService {

    private static final Pattern INJECTION = Pattern.compile(
            "(?i)(ignore\\s+(all\\s+)?previous|system\\s+prompt|you\\s+are\\s+now|jailbreak|DAN\\s+mode)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OFFENSIVE = Pattern.compile(
            "(?i)\\b(abuse|kill|suicide)\\b"
    );

    private final int maxLength;

    public ChatSafetyService(AppProperties props) {
        this.maxLength = props.getChat().getMaxMessageLength();
    }

    public String sanitizeUserMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ChatException("Message cannot be empty.");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > maxLength) {
            throw new ChatException("Message is too long (max " + maxLength + " characters).");
        }
        if (INJECTION.matcher(trimmed).find()) {
            throw new ChatException("Invalid message content.");
        }
        if (OFFENSIVE.matcher(trimmed).find()) {
            throw new ChatException("Please keep the conversation respectful and counselling-related.");
        }
        return trimmed;
    }

    public boolean looksLikeSpam(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (message.length() < 3) {
            return true;
        }
        long unique = message.chars().distinct().count();
        return message.length() > 20 && unique < 4;
    }
}
