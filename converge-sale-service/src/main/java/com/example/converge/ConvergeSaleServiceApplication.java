package com.example.converge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class ConvergeSaleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConvergeSaleServiceApplication.class, args);
    }
}


