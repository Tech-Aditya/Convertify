package com.convertify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ConvertifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConvertifyApplication.class, args);
    }
}
