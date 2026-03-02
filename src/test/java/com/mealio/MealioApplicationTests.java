package com.mealio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class MealioApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context initializes cleanly in dev (H2) profile.
    }
}
