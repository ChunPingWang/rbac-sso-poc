package contracts.auditquery

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return audit logs by username"
    description "Returns paginated audit logs filtered by username"

    request {
        method GET()
        url "/api/v1/audit-logs"
        urlPath("/api/v1/audit-logs") {
            queryParameters {
                parameter 'username': 'admin@example.com'
                parameter 'page': '0'
                parameter 'size': '20'
            }
        }
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
            content: [
                [
                    id: "550e8400-e29b-41d4-a716-446655440000",
                    eventType: "PRODUCT_CREATED",
                    aggregateType: "Product",
                    username: "admin@example.com",
                    result: "SUCCESS"
                ],
                [
                    id: "550e8400-e29b-41d4-a716-446655440001",
                    eventType: "PRODUCT_UPDATED",
                    aggregateType: "Product",
                    username: "admin@example.com",
                    result: "SUCCESS"
                ]
            ],
            page: 0,
            size: 20,
            totalElements: 2,
            totalPages: 1
        ])
        bodyMatchers {
            jsonPath('$.content', byType { minOccurrence(1) })
            jsonPath('$.content[*].id', byRegex('[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}'))
            jsonPath('$.content[*].username', byRegex('[a-zA-Z0-9.@_-]+'))
            jsonPath('$.page', byRegex('[0-9]+'))
            jsonPath('$.size', byRegex('[0-9]+'))
            jsonPath('$.totalElements', byRegex('[0-9]+'))
            jsonPath('$.totalPages', byRegex('[0-9]+'))
        }
    }
}
