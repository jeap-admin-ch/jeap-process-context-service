package ch.admin.bit.jeap.processcontext.adapter.jpa;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Duration;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories
@EnableJpaAuditing
@EnableCaching
@ComponentScan
@EntityScan(basePackages = "ch.admin.bit.jeap.processcontext")
public class JpaAdapterConfig {

    static final String PROCESS_INSTANCE_ID_BY_ORIGIN_PROCESS_ID_CACHE = "processInstanceIdByOriginProcessId";

    @Bean
    Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(1024)
                .initialCapacity(128)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats();
    }

    @Bean
    CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager(
                PROCESS_INSTANCE_ID_BY_ORIGIN_PROCESS_ID_CACHE);
        caffeineCacheManager.setAllowNullValues(false);
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }
}
