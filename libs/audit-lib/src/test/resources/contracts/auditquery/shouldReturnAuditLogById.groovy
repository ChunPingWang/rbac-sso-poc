package contracts.auditquery

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return audit log by ID"
    description "Returns a single audit log when queried by valid UUID"

    request {
        method GET()
        url "/api/v1/audit-logs/550e8400-e29b-41d4-a716-446655440000"
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
            id: "550e8400-e29b-41d4-a716-446655440000",
            timestamp: "2026-01-10T08:30:00Z",
            eventType: "PRODUCT_CREATED",
            aggregateType: "Product",
            aggregateId: "prod-12345",
            username: "admin@example.com",
            serviceName: "product-service",
            action: "createProduct",
            payload: '{"productCode":"TEST-001","productName":"Test Widget"}',
            result: "SUCCESS",
            clientIp: "192.168.1.100",
            correlationId: "corr-abc-123",
            payloadTruncated: false
        ])
        bodyMatchers {
            jsonPath('$.id', byRegex('[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}'))
            jsonPath('$.timestamp', byRegex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z'))
            jsonPath('$.eventType', byRegex('[A-Z_]+'))
            jsonPath('$.result', byRegex('SUCCESS|FAILURE'))
        }
    }
}
