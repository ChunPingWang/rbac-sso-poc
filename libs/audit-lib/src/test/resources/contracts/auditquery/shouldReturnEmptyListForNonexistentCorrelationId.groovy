package contracts.auditquery

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return empty list for nonexistent correlation ID"
    description "Returns empty array when no audit logs match the correlation ID"

    request {
        method GET()
        url "/api/v1/audit-logs/correlation/nonexistent"
        headers {
            contentType applicationJson()
        }
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body([])
    }
}
