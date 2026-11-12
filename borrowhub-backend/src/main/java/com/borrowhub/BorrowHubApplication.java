package com.borrowhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.TimeZone;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableTransactionManagement
public class BorrowHubApplication {

    public static void main(String[] args) {
        // Enforce institutional default timezone on JVM startup
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(BorrowHubApplication.class, args);
    }
}
