package com.shop.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI shopOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BD Project Market API")
                        .version("v1")
                        .description("Backend для BD_Project_Market"));
    }
}
