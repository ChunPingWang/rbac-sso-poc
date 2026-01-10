package com.example.ecommerce.tests;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cucumber hooks for test setup and teardown
 */
public class CucumberHooks {

    @Autowired
    private TestContext testContext;

    @Before
    public void resetContext() {
        testContext.reset();
    }
}
