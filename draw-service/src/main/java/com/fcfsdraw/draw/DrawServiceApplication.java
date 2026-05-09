package com.fcfsdraw.draw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DrawServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrawServiceApplication.class, args);
    }
}
