package com.hwz.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwz.common.Result;
import com.hwz.common.auth.AccessTokenService;
import com.hwz.common.context.BaseContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AuthContextFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";

    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    public AuthContextFilter(AccessTokenService accessTokenService, ObjectMapper objectMapper) {
        this.accessTokenService = accessTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (StringUtils.hasText(contextPath) && path != null && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            if (path != null && path.startsWith("/api") && !path.startsWith("/api/auth")) {
                String authorization = request.getHeader(AUTH_HEADER);
                if (!StringUtils.hasText(authorization)) {
                    writeError(response, HttpStatus.UNAUTHORIZED.value(), "Please log in first");
                    return;
                }
                try {
                    Long userId = accessTokenService.verifyAndGetUserId(authorization);
                    BaseContext.setCurrentId(userId);
                } catch (ResponseStatusException ex) {
                    writeError(response, ex.getStatus().value(), ex.getReason());
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            BaseContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), Result.fail(
                StringUtils.hasText(message) ? message : HttpStatus.valueOf(status).getReasonPhrase()));
    }
}
