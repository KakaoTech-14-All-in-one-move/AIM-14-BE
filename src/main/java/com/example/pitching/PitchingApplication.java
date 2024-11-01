package com.example.pitching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class PitchingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PitchingApplication.class, args);
    }

}
