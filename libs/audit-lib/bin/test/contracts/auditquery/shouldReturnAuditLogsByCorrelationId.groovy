package contracts.auditquery

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return audit logs by correlation ID"
    description "Returns all audit logs linked by the same correlation ID for distributed tracing"

    request {
        method GET()
        url "/api/v1/audit-logs/correlation/corr-abc-123"
        headers {
            contentType applicationJson()
        }
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body([
            [
                id: "550e8400-e29b-41d4-a716-446655440000",
                timestamp: "2026-01-10T08:30:00Z",
                eventType: "PRODUCT_CREATED",
                aggregateType: "Product",
                aggregateId: "prod-12345",
                username: "admin@example.com",
                serviceName: "product-service",
                correlationId: "corr-abc-123",
                result: "SUCCESS"
            ],
            [
                id: "550e8400-e29b-41d4-a716-446655440001",
                timestamp: "2026-01-10T08:31:00Z",
                eventType: "PRODUCT_UPDATED",
                aggregateType: "Product",
                aggregateId: "prod-12345",
                username: "admin@example.com",
                serviceName: "product-service",
                correlationId: "corr-abc-123",
                result: "SUCCESS"
            ]
        ])
        bodyMatchers {
            jsonPath('$', byType { minOccurrence(1) })
            jsonPath('$[*].id', byRegex('[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}'))
            jsonPath('$[*].correlationId', byRegex('corr-[a-z0-9-]+'))
        }
    }
}
