package com.hwz.common.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class AccessTokenService {

    private static final String PREFIX = "token-";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final long ttlSeconds;

    public AccessTokenService(
            @Value("${labcore.auth.access-token-secret:change-this-local-dev-token-secret}") String secret,
            @Value("${labcore.auth.access-token-ttl-seconds:7200}") long ttlSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid userId");
        }
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = userId + ":" + expiresAt + ":" + UUID.randomUUID();
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return PREFIX + encodedPayload + "." + sign(encodedPayload);
    }

    public Long verifyAndGetUserId(String rawToken) {
        String token = stripBearer(rawToken);
        if (!StringUtils.hasText(token) || !token.startsWith(PREFIX)) {
            throw unauthorized();
        }

        String body = token.substring(PREFIX.length());
        int dot = body.indexOf('.');
        if (dot <= 0 || dot == body.length() - 1) {
            throw unauthorized();
        }

        String encodedPayload = body.substring(0, dot);
        String signature = body.substring(dot + 1);
        if (!constantTimeEquals(signature, sign(encodedPayload))) {
            throw unauthorized();
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw unauthorized();
        }

        String[] parts = payload.split(":", 3);
        if (parts.length != 3) {
            throw unauthorized();
        }

        try {
            Long userId = Long.parseLong(parts[0]);
            long expiresAt = Long.parseLong(parts[1]);
            if (userId <= 0 || expiresAt < Instant.now().getEpochSecond()) {
                throw unauthorized();
            }
            return userId;
        } catch (NumberFormatException ex) {
            throw unauthorized();
        }
    }

    private String stripBearer(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        return authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length())
                : authorization;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return base64Url(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign access token", ex);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] a = left == null ? new byte[0] : left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right == null ? new byte[0] : right.getBytes(StandardCharsets.UTF_8);
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
}
