---
name: init-project
description: Guide a fresh cloner of this template through filling in project-specific details — project name, owner, chosen compliance regimes, identity provider, tokenization/SIEM/observability vendors, and data residency — across the template's docs. Use right after cloning this blueprint, or when asked to initialize/set up the project.
---

# init-project

Walk a team that just cloned this blueprint through making it *theirs*. The
template ships a vendor-agnostic reference design (ADR-0001) with several
decisions left open; this skill collects the choices and threads them through the
docs.

## 1. Confirm context

Open and skim with the user:
- `README.md`, `docs/README.md`
- ADR-0001 (`docs/adrs/0001-secure-mcp-service-design.md`) — especially §1 (Scope &
  Decisions) and §28 (Open Items / Next Decisions).

## 2. Collect decisions

Ask for these (batch the questions; accept "decide later" and leave the open item
checked):

| Topic | What to capture |
|---|---|
| **Project / org** | Project name, owning team, accountable `owner`. |
| **Compliance regimes** | Which of GDPR/LGPD/HIPAA/PCI-DSS/SOC2/FedRAMP/EU-AI-Act/NIST-AI-RMF/ISO-42001 actually apply. Drop the rest. |
| **Identity provider** | Which OIDC IdP(s) and federation pattern (broker vs. gateway-validates-each). |
| **Tokenization vendor** | Managed tokenization service (PCI scope reduction, multi-region). |
| **SIEM** | Splunk vs. Sentinel vs. other (affects OTel exporter config). |
| **Observability backends** | Which OTLP backends, self-hosted vs. SaaS per region. |
| **WORM / KMS** | Audit store and HSM-backed KMS per region. |
| **Data residency** | Which regions (EU/Brazil/US/…) are in scope. |
| **Audit retention** | Confirm 7-year default or set another with legal. |

## 3. Apply the choices

- **ADR-0001 frontmatter & §1:** trim `regimes:` and the Scope table to the
  regimes/decisions actually chosen. Set `owner:`. Bump `version` and `date` per
  `docs/process/adr-process.md` §5; note the change.
- **ADR-0001 §28 Open Items:** for each decision made, check the box and record
  the choice inline; leave deferred ones unchecked.
- **ADR index** (`docs/adrs/README.md`): update ADR-0001's row (regimes, owner,
  version, status).
- **README.md / docs/README.md:** replace the generic "secure MCP service"
  framing with the project name where it reads as a placeholder.
- **CLAUDE.md:** narrow the regime list in the opening paragraph to the chosen
  set, if the team wants the agent guidance scoped down.

## 4. Hand off

- Recommend running `review-design-doc` and then `compliance-check` on ADR-0001
  now that regimes are finalized.
- List the remaining open items (deferred decisions) so the team knows what's
  still owed.
- Remind them: named vendors stay framed as interchangeable examples unless the
  team is deliberately committing — keep the vendor-agnostic posture intact.

## Rules

- Don't invent decisions. If the user defers, leave the open item and move on.
- Preserve ADR-0001's structure and house style; you're filling it in, not
  rewriting it.
- Follow versioning rules in `docs/process/adr-process.md`.
