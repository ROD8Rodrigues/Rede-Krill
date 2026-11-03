package com.redekrill.scheduling;

import com.redekrill.scheduling.infrastructure.config.properties.AuthProperties;
import com.redekrill.scheduling.infrastructure.config.properties.CorsProperties;
import com.redekrill.scheduling.infrastructure.config.properties.EmailOutboxProperties;
import com.redekrill.scheduling.infrastructure.config.properties.EmailProperties;
import com.redekrill.scheduling.infrastructure.config.properties.JwtProperties;
import com.redekrill.scheduling.infrastructure.config.properties.SecurityProperties;
import com.redekrill.scheduling.infrastructure.config.properties.SeedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        AuthProperties.class,
        SecurityProperties.class,
        CorsProperties.class,
        EmailProperties.class,
        EmailOutboxProperties.class,
        SeedProperties.class
})
public class SchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulingApplication.class, args);
    }
}
