package gr.aegean.cinema.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for:
 * - Screening submissions: max 10 per minute per IP
 * - Search endpoints: max 30 per minute per IP
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> submissionBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> searchBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);

        // Rate limit screening submissions (POST /api/screenings or PATCH submit/final-submit)
        boolean isSubmission = (method.equals("POST") && path.equals("/api/screenings"))
                || (method.equals("PATCH") && (path.contains("/submit") || path.contains("/final-submit")));

        // Rate limit search endpoints
        boolean isSearch = method.equals("GET") && (path.contains("/search") || path.contains("/announced"));

        if (isSubmission) {
            Bucket bucket = submissionBuckets.computeIfAbsent(clientIp, k -> createSubmissionBucket());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for submissions from IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"message\":\"Submission rate limit exceeded. Max 10 per minute.\"}");
                return;
            }
        } else if (isSearch) {
            Bucket bucket = searchBuckets.computeIfAbsent(clientIp, k -> createSearchBucket());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for searches from IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"message\":\"Search rate limit exceeded. Max 30 per minute.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createSubmissionBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createSearchBucket() {
        Bandwidth limit = Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
