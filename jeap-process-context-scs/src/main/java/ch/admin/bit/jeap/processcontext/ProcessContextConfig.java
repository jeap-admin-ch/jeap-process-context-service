package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.plugin.api.relation.LoggingRelationListener;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30m")
@ComponentScan(basePackageClasses = Application.class)
@PropertySource("classpath:processContextDefaultProperties.properties")
class ProcessContextConfig {

    @Bean
    @ConditionalOnMissingBean(RelationListener.class)
    RelationListener loggingRelationListener() {
        return new LoggingRelationListener();
    }

    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}

