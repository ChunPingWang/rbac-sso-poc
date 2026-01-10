package com.example.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.example.ecommerce.product",
    "com.example.ecommerce.security",
    "com.example.ecommerce.tenant",
    "com.example.audit"
})
@EnableJpaRepositories(basePackages = {
    "com.example.ecommerce.product.adapter.outbound.persistence"
})
@EntityScan(basePackages = {
    "com.example.ecommerce.product.adapter.outbound.persistence.entity",
    "com.example.audit.infrastructure.persistence.entity"
})
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
