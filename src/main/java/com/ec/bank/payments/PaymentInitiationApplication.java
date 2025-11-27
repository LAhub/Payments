package com.ec.bank.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Main Spring Boot application class.
 * Entry point for Payment Initiation Service.
 */
@SpringBootApplication
@EnableR2dbcRepositories
public class PaymentInitiationApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentInitiationApplication.class, args);
    }
}
