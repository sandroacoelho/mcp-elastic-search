# Evidence Calendar

Required by [ADR-0001 §14 + §22](../adrs/0001-secure-mcp-service-design.md). For
each control, what evidence proves it operated, how often it's collected, who owns
it, and where it's stored (system of record). Rows seeded from the §22 evidence
matrix — assign owners, systems of record, and dates.

> Status: **TEMPLATE — cadence pre-filled from §22; owners/dates pending.**

| Control | Evidence | Frequency | Owner (A) | System of record | Last collected | Next due |
|---------|----------|-----------|-----------|------------------|----------------|----------|
| OPA deny-by-default | Policy test results, signed bundle metadata, deployment approval | Every policy change | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| Gateway-only access | Network-policy tests, denied direct-ingress attempts | Continuous + quarterly | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| Tool registry signing | Manifest signature verification logs | Every deployment | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| Human approval | Approval records (request hash + approver id) | Every high-risk action | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| DLP / tokenization | DLP scan logs, tokenization metrics, exception review | Continuous + monthly | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| Access reviews | Completed access-certification exports | Quarterly | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| Key management | Rotation logs, HSM/KMS access logs | Per rotation + quarterly | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| Audit immutability | WORM lock evidence, hash-chain + RFC 3161 anchor verification | Monthly / quarterly | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| Vulnerability management | Scan results, remediation tickets, exception approvals | Continuous + monthly | _tbd_ | _tbd_ | _tbd_ | _tbd_ |
| Incident response | Tabletop records, incident tickets, post-incident reports | Annual + per incident | _tbd_ | _tbd_ | _tbd_ | _tbd_ |

**Owner** here is the **A** from [`raci-matrix.md`](./raci-matrix.md) for that
control family. Overdue rows are a compliance gap — surface them in the monthly
review (§22).
