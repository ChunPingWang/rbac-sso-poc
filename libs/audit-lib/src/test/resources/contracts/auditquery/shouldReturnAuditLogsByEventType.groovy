package contracts.auditquery

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return audit logs by event type"
    description "Returns paginated audit logs filtered by event type"

    request {
        method GET()
        url "/api/v1/audit-logs"
        urlPath("/api/v1/audit-logs") {
            queryParameters {
                parameter 'eventType': 'PRODUCT_CREATED'
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
                    result: "SUCCESS"
                ]
            ],
            page: 0,
            size: 20,
            totalElements: 1,
            totalPages: 1
        ])
        bodyMatchers {
            jsonPath('$.content[*].eventType', byRegex('[A-Z_]+'))
            jsonPath('$.page', byRegex('[0-9]+'))
            jsonPath('$.totalElements', byRegex('[0-9]+'))
        }
    }
}
