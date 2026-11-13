package com.homeq.alert.config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Configuration
@EnableConfigurationProperties(AlertProperties.class)
public class PropertiesConfig { }
