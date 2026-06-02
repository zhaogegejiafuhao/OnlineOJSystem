package cn.edu.zjnu.acm.interceptor;

import com.alibaba.fastjson.JSON;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class AIRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, RateLimitWindow> userWindows = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getSession().getId();
        if (request.getSession().getAttribute("currentUser") != null) {
            userId = request.getSession().getAttribute("currentUser").hashCode() + "";
        }

        RateLimitWindow window = userWindows.computeIfAbsent(userId, k -> new RateLimitWindow());
        long now = System.currentTimeMillis();

        synchronized (window) {
            if (now - window.startTime > WINDOW_MS) {
                window.startTime = now;
                window.count.set(0);
            }

            if (window.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                log.warn("AI rate limit exceeded for user: {}, count: {}", userId, window.count.get());
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(429);
                response.getWriter().println(
                    JSON.toJSONString(new RestfulResult(429, "请求过于频繁，请稍后再试（每分钟最多" + MAX_REQUESTS_PER_MINUTE + "次）"))
                );
                return false;
            }
        }

        return true;
    }

    private static class RateLimitWindow {
        long startTime = System.currentTimeMillis();
        AtomicLong count = new AtomicLong(0);
    }
}
