package com.hwz.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证 AI 单用户并发限制和一分钟请求窗口，防止限流逻辑回归。 */
class AiRequestGuardTest {

    @Test
    void shouldRejectSecondConcurrentRequestAndReleasePermit() {
        AiRequestGuard guard = configuredGuard(10, 1);
        AiRequestGuard.Permit first = guard.acquire(7L);
        assertThrows(ResponseStatusException.class, () -> guard.acquire(7L));
        first.close();
        assertDoesNotThrow(() -> guard.acquire(7L).close());
    }

    @Test
    void shouldRejectRequestsBeyondMinuteLimit() {
        AiRequestGuard guard = configuredGuard(2, 2);
        guard.acquire(7L).close();
        guard.acquire(7L).close();
        assertThrows(ResponseStatusException.class, () -> guard.acquire(7L));
    }

    private AiRequestGuard configuredGuard(int perMinute, int concurrent) {
        AiRequestGuard guard = new AiRequestGuard();
        ReflectionTestUtils.setField(guard, "requestsPerMinute", perMinute);
        ReflectionTestUtils.setField(guard, "maxConcurrentPerUser", concurrent);
        return guard;
    }
}
