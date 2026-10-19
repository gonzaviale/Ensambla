package com.ensambla.backend.infrastructure.whatsapp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WhatsappProperties.class)
public class WhatsappConfig {
}
