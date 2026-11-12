package com.borrowhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class BorrowHubApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring Boot context loading, JPA entity mappings, and Flyway migration execution
    }
}
