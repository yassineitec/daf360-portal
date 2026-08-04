package com.daf360.portal.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * `anniversaries` caches the birthday / work-anniversary feed per (pays, from, to).
     *
     * That feed is the expensive part of GET /api/portal/events, and the home page triggers
     * the whole merge TWICE per load — once for the visible month and once for
     * /events/upcoming?days=30, which internally calls the same range method. Anniversaries
     * are derived from dates of birth and hire dates, so they do not change within a
     * 5-minute window; caching them turns the second call (and every revisit) into a map
     * lookup. A changed profile is picked up on the next expiry.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userInfo", "anniversaries");
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
        );
        return manager;
    }
}
