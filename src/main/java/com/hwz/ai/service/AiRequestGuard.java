package com.hwz.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 请求保护器：限制单用户每分钟请求数和同时运行的请求数。
 * 状态只保存在当前应用实例内，适合本地单机部署；未来多实例部署可替换为 Redis。
 */
@Component
public class AiRequestGuard {
    private final Map<Long, UserWindow> windows = new ConcurrentHashMap<>();

    @Value("${labcore.ai.rate-limit-per-minute:12}")
    private int requestsPerMinute;
    @Value("${labcore.ai.max-concurrent-per-user:2}")
    private int maxConcurrentPerUser;

    public Permit acquire(Long userId) {
        Instant now = Instant.now();
        UserWindow[] acquired = new UserWindow[1];
        // compute 将同一用户的“取得窗口 + 增加并发数”变成原子操作，避免并发清理造成限流旁路。
        windows.compute(userId, (key, existing) -> {
            UserWindow window = existing == null ? new UserWindow() : existing;
            Instant cutoff = now.minusSeconds(60);
            synchronized (window) {
                while (!window.requestTimes.isEmpty() && window.requestTimes.peekFirst().isBefore(cutoff)) {
                    window.requestTimes.removeFirst();
                }
                if (window.requestTimes.size() >= Math.max(1, requestsPerMinute)) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI 请求过于频繁，请稍后再试");
                }
                if (window.concurrent >= Math.max(1, maxConcurrentPerUser)) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "已有 AI 回答正在生成，请等待完成");
                }
                window.requestTimes.addLast(now);
                window.concurrent++;
                acquired[0] = window;
            }
            return window;
        });
        cleanupExpiredWindows(now.minusSeconds(60), userId);
        return new Permit(userId, acquired[0]);
    }

    /** 新请求到达时顺便清理一分钟前的空闲用户窗口，避免本地长时间运行后持续占用内存。 */
    private void cleanupExpiredWindows(Instant cutoff, Long activeUserId) {
        windows.forEach((id, ignored) -> {
            if (id.equals(activeUserId)) return;
            windows.computeIfPresent(id, (key, window) -> {
                synchronized (window) {
                    if (window.concurrent == 0 && (window.requestTimes.isEmpty()
                            || window.requestTimes.peekLast().isBefore(cutoff))) {
                        return null;
                    }
                    return window;
                }
            });
        });
    }

    public final class Permit implements AutoCloseable {
        private final Long userId;
        private final UserWindow window;
        private boolean closed;

        private Permit(Long userId, UserWindow window) {
            this.userId = userId;
            this.window = window;
        }

        @Override
        public void close() {
            synchronized (window) {
                if (closed) return;
                closed = true;
                window.concurrent = Math.max(0, window.concurrent - 1);
                if (window.concurrent == 0 && window.requestTimes.isEmpty()) windows.remove(userId, window);
            }
        }
    }

    private static final class UserWindow {
        private final Deque<Instant> requestTimes = new ArrayDeque<>();
        private int concurrent;
    }
}
