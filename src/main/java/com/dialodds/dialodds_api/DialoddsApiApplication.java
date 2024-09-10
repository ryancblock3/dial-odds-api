package com.dialodds.dialodds_api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class DialoddsApiApplication {

    private static final Logger logger = LoggerFactory.getLogger(DialoddsApiApplication.class);

    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.configure()
                              .directory("./")  // Look in the current directory
                              .ignoreIfMissing()
                              .load();
        
        // Set environment variables
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
            logger.info("Setting env variable: {} = {}", entry.getKey(), maskSensitiveValue(entry.getKey(), entry.getValue()));
        });
        
        SpringApplication.run(DialoddsApiApplication.class, args);
    }

    private static String maskSensitiveValue(String key, String value) {
        if (key.toLowerCase().contains("password") || key.toLowerCase().contains("secret")) {
            return "*****";
        }
        return value;
    }
}