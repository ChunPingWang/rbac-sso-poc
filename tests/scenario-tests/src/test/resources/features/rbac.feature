# language: zh-TW
功能: 角色權限控制
  作為系統
  我需要根據使用者角色控制存取權限
  以確保系統安全

  場景大綱: 角色存取控制
    假設 使用者 "<使用者>" 已登入系統，角色為 "<角色>"
    當 使用者嘗試存取 "<端點>"
    那麼 系統應回傳 "<結果>"

    例子:
      | 使用者        | 角色         | 端點              | 結果     |
      | admin        | ADMIN        | /api/products     | 200      |
      | admin        | ADMIN        | /api/admin/users  | 200      |
      | tenant-admin | TENANT_ADMIN | /api/products     | 200      |
      | tenant-admin | TENANT_ADMIN | /api/admin/users  | 403      |
      | user         | USER         | /api/products     | 200      |
      | user         | USER         | /api/products/new | 403      |
      | viewer       | VIEWER       | /api/products     | 200      |
      | viewer       | VIEWER       | /api/products/new | 403      |

  場景: 未認證使用者無法存取受保護端點
    假設 使用者未登入
    當 使用者嘗試存取 "/api/products"
    那麼 系統應回傳 "401"

  場景: 無效 Token 被拒絕
    假設 使用者持有無效的 JWT Token
    當 使用者嘗試存取 "/api/products"
    那麼 系統應回傳 "401"
