package com.dialodds.dialodds_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI dialOddsOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("DialOdds API")
                        .description("NFL betting application API")
                        .version("v0.0.1"));
    }
}