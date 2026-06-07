package com.rankwise.chat.service;

import com.rankwise.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatRateLimitService {

    private final int limitPerHour;
    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public ChatRateLimitService(AppProperties props) {
        this.limitPerHour = Math.max(10, props.getChat().getRateLimitPerHour());
    }

    public void check(String key) {
        long now = Instant.now().toEpochMilli();
        long hourAgo = now - 3_600_000L;
        Deque<Long> times = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < hourAgo) {
                times.pollFirst();
            }
            if (times.size() >= limitPerHour) {
                throw new ChatException("Too many messages. Please wait a while and try again.");
            }
            times.addLast(now);
        }
    }
}
