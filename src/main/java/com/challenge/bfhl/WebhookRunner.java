package com.challenge.bfhl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WebhookRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(WebhookRunner.class);

    @Autowired
    private WebhookService webhookService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            logger.info("Starting webhook process on application startup");
            webhookService.processWebhook();
        } catch (Exception e) {
            logger.error("Error in webhook process: {}", e.getMessage(), e);
        }
    }
}