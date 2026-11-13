package com.homeq.alert.config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Repeating checks are only enabled in the normal long-running mode, not with homeq.run-once=true. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name="homeq.run-once",havingValue="false",matchIfMissing=true)
public class SchedulingConfig { }
