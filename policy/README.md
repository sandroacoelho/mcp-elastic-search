# Policy — OPA / Rego authorization (PDP)

The runnable form of the authorization decision in
[ADR-0001 §5](../docs/adrs/0001-secure-mcp-service-design.md). OPA is the **PDP**
(Policy Decision Point); the gateway is the **PEP** that calls it on every tool
invocation and enforces the returned `decision`.

## Files

| File | Purpose |
|---|---|
| `authz.rego` | The policy: deny-by-default, read tier, high-risk tiers (step-up + human approval), context-bound approval, obligations. |
| `authz_test.rego` | Tests for allow / deny / pending-approval / obligation / fail-closed (ADR-0001 §15, §23/§758 QA matrix). |
| `authz_elastic_test.rego` | Read-tier authorization tests for the Elasticsearch server's five tools (ADR-0003): allowlist scoping, result cap + sensitive-field projection obligations, restricted-data approval gating. |
| `data.json` | **Example** RBAC + purpose catalog (incl. example ES index grants and `read_row_cap`). Replace with your real data — the policy is generic; the data is per-deployment. |

## Decision contract

Input (from §5): `{ user{role}, tool{tier,resource}, purpose, data_classification,
approval{...}, request{context_hash} }`.

Output:

```json
{ "effect": "deny" | "allow" | "pending_approval", "obligations": [ ... ] }
```

- **deny** — no matching grant (deny-by-default). The gateway blocks.
- **pending_approval** — high-risk tier authorized in principle, but execution must
  pause for step-up auth + human approval; obligations name what's required.
- **allow** — proceed, applying every obligation (e.g. `tokenize pan`, `cap_rows`).

## Run the tests

```bash
opa test policy/ -v        # unit tests
opa check policy/          # compile / type check
opa fmt --list policy/     # style (empty output = clean)
```

CI runs these on every PR — see the `policy-test` job in
[`../.github/workflows/supply-chain.yml`](../.github/workflows/supply-chain.yml).

## Notes

- `role_grants` / `valid_purpose` come from `data.mcp.config` (the example
  `data.json`), kept separate from the `mcp.authz` policy package so the same
  policy serves every deployment — swap the data, not the code.
- Approval is **bound to the exact request** (`bound_context_hash ==
  request.context_hash`): an approval issued for one request cannot authorize a
  different one (§15).
- This is the authorization core only. DLP/tokenization execution, token
  exchange, and step-up/approval orchestration live in the gateway (§5–§7).
