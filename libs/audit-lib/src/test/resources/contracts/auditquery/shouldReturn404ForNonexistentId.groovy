package contracts.auditquery

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return 404 for nonexistent audit log ID"
    description "Returns 404 Not Found when audit log with given ID does not exist"

    request {
        method GET()
        url "/api/v1/audit-logs/00000000-0000-0000-0000-000000000000"
        headers {
            contentType applicationJson()
        }
    }

    response {
        status NOT_FOUND()
    }
}
