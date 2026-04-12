package com.daniphord.mahanga.Config;

import com.daniphord.mahanga.Util.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide bean configuration
 */
@Configuration
public class AppConfig {

    /**
     * Rate limiter: 5 attempts per 5 minutes, 15 minute lockout
     */
    @Bean
    public RateLimiter loginRateLimiter() {
        return new RateLimiter(5, 300, 900);
    }
}

