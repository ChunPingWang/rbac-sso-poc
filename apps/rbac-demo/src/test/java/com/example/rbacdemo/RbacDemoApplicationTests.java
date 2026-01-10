package com.example.rbacdemo;

import com.example.rbacdemo.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class RbacDemoApplicationTests {

    @Test
    void contextLoads() {
        // Context load test
    }
}
