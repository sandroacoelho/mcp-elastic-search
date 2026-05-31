# Exception Register

Required by [ADR-0001 §14](../adrs/0001-secure-mcp-service-design.md). Records
deliberate, time-boxed deviations from a control, each with a **compensating
control**, an **owner**, an **approver**, and a hard **expiry**. An exception
without an expiry is a finding, not an exception.

> Status: **TEMPLATE — example row below; replace before use.**

| ID | Control deviated | Reason / justification | Compensating control | Risk ref | Owner | Approved by | Granted | Expiry | Status |
|----|------------------|------------------------|----------------------|----------|-------|-------------|---------|--------|--------|
| E-001 | _e.g._ Egress allowlist temporarily widened for vendor X migration | Time-boxed data migration | Extra DLP sampling + daily egress log review | R-002 | _tbd_ | _tbd_ | _YYYY-MM-DD_ | _YYYY-MM-DD_ | open |

**Rules:**
- Exactly one approver, independent of the requester (separation of duties, §15).
- Every exception links to a risk-register entry ([`risk-register.md`](./risk-register.md)).
- Review all open exceptions monthly; auto-escalate any past expiry.
- Closing an exception requires evidence the control is restored.
