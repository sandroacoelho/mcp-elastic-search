# MCP authorization policy — the PDP (Policy Decision Point) from ADR-0001 §5.
#
# The gateway (PEP) calls this on every tool invocation with the policy input
# from §5:
#   { user{role, residency}, tool{tier, resource}, purpose, data_classification,
#     region, cross_region_legal_basis, approval{...}, request{context_hash} }
#
# Output is a single `decision` object:
#   { "effect": "deny" | "allow" | "pending_approval", "obligations": <set> }
#
# Design rules embodied here:
#   - Deny-by-default; only a known tier + grant + valid purpose can authorize.  §5
#   - Region pinning: processing region must match the subject's residency,
#     unless an explicit legal basis is asserted.                                §12
#   - Classification fail-safe: missing/unknown data_classification is treated
#     as Restricted and blocked unless explicitly granted + approved.            §24
#   - High-risk tiers AND any Restricted-class data require step-up + human
#     approval, bound to the exact request context.                             §7, §15, §24
#   - Obligations (tokenize PAN, cap rows, redact Restricted) ride with allow.   §6, §24
#
# role_grants / valid_purpose are data-driven (see data.json) so the same policy
# serves every deployment — swap the data, not the code.
package mcp.authz

import rego.v1

# Deny-by-default. Overridden only when a rule below produces a decision.
default decision := {"effect": "deny", "obligations": set()}

high_risk_tiers := {"write", "external", "bulk", "file-write"}

# Recognized tiers. An unknown tier is never authorized (fail closed).
known_tiers := {"read"} | high_risk_tiers

role_grants := data.mcp.config.role_grants

valid_purpose := data.mcp.config.valid_purposes

# ---- Data classification (§24), with fail-safe default to Restricted ---------
restricted_classes := {"restricted", "PHI", "PCI", "special-category"}

confidential_classes := {"confidential", "PII"}

low_classes := {"public", "internal"}

all_known_classes := (restricted_classes | confidential_classes) | low_classes

# True only when the request carries a recognized classification. Defaulting to
# false makes a MISSING classification fall through to the fail-safe below
# (note: `not (undefined in set)` is itself undefined in Rego, so we can't rely
# on negating the membership directly).
default has_known_class := false

has_known_class if input.data_classification in all_known_classes

# Missing OR unrecognized classification -> Restricted (fail-safe, §24).
effective_level := "restricted" if not has_known_class

effective_level := "restricted" if input.data_classification in restricted_classes

effective_level := "confidential" if input.data_classification in confidential_classes

effective_level := "low" if input.data_classification in low_classes

# ---- Region pinning (§12) ----------------------------------------------------
# Processing region must equal the subject's residency, unless an explicit legal
# basis is asserted. Missing residency or region => not ok => deny.
region_ok if input.user.residency == input.region

region_ok if input.cross_region_legal_basis == true

# ---- Base authorization ------------------------------------------------------
authorized if {
	known_tiers[input.tool.tier]
	role_grants[input.user.role][input.tool.resource]
	valid_purpose[input.purpose]
	region_ok
}

# Approval is required for any high-risk tier OR any Restricted-class data (§7, §24).
requires_approval if high_risk_tiers[input.tool.tier]

requires_approval if effective_level == "restricted"

# ---- Decisions (mutually exclusive; default deny otherwise) ------------------
# Authorized and no approval needed -> allow.
decision := {"effect": "allow", "obligations": obligations} if {
	authorized
	not requires_approval
}

# Authorized but approval needed and not yet satisfied -> pause for step-up + approval.
decision := {"effect": "pending_approval", "obligations": approval_obligations} if {
	authorized
	requires_approval
	not valid_bound_approval
}

# Authorized, approval needed, and a valid context-bound approval is present -> allow.
decision := {"effect": "allow", "obligations": obligations} if {
	authorized
	requires_approval
	valid_bound_approval
}

# A valid approval is step-up-verified, human-approved, single-use, unexpired, and
# bound to this exact request context (§15). Any mismatch fails the binding.
valid_bound_approval if {
	input.approval.step_up_verified == true
	input.approval.human_approved == true
	input.approval.bound_context_hash == input.request.context_hash
	input.approval.single_use == true
	input.approval.not_expired == true
}

approval_obligations := {"require_step_up", "require_human_approval"}

# ---- Obligations the gateway MUST enforce alongside an allow (§6, §24) -------
obligations contains {"action": "tokenize", "field": "pan"} if input.data_classification == "PCI"

obligations contains {"action": "cap_rows", "limit": 1000} if input.tool.tier == "bulk"

# Restricted data (other than PCI, which is already tokenized) must be redacted/minimized.
obligations contains {"action": "redact", "scope": "restricted"} if {
	effective_level == "restricted"
	not input.data_classification == "PCI"
}
