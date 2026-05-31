# Tests for the MCP authorization policy.
# Covers allow / deny / pending-approval / obligation / fail-closed cases,
# including region pinning (§12) and the classification fail-safe (§24), as
# required by ADR-0001 §15 ("deny-by-default behavior is covered by automated
# tests") and the §23 / §758 QA acceptance matrix.
#
#   opa test policy/ -v
#
# Fixtures use the example data in data.json (role "analyst" grants "orders" and
# "customer_record"; valid purposes include "fraud_review").
package mcp.authz_test

import rego.v1

import data.mcp.authz

# ---- fixtures ----------------------------------------------------------------

# An authorized, same-region, low-classification read (no approval needed).
read_input := {
	"user": {"role": "analyst", "residency": "EU"},
	"region": "EU",
	"tool": {"tier": "read", "resource": "orders"},
	"purpose": "fraud_review",
	"data_classification": "internal",
}

# A high-risk write with a fully valid, context-bound approval (low-class data,
# same region, so only the tier drives the approval requirement).
approved_write := {
	"user": {"role": "analyst", "residency": "EU"},
	"region": "EU",
	"tool": {"tier": "write", "resource": "orders"},
	"purpose": "fraud_review",
	"data_classification": "internal",
	"request": {"context_hash": "abc123"},
	"approval": {
		"step_up_verified": true,
		"human_approved": true,
		"bound_context_hash": "abc123",
		"single_use": true,
		"not_expired": true,
	},
}

# ---- deny-by-default / fail-closed -------------------------------------------

test_default_deny_on_empty_input if {
	authz.decision.effect == "deny" with input as {}
}

test_unknown_tier_fails_closed if {
	authz.decision.effect == "deny" with input as object.union(read_input, {"tool": {"tier": "teleport", "resource": "orders"}})
}

# ---- read tier ---------------------------------------------------------------

test_read_allowed_with_grant_purpose_region_and_class if {
	authz.decision.effect == "allow" with input as read_input
}

test_read_denied_when_purpose_invalid if {
	authz.decision.effect == "deny" with input as object.union(read_input, {"purpose": "snooping"})
}

test_read_denied_when_role_lacks_resource if {
	authz.decision.effect == "deny" with input as object.union(read_input, {"tool": {"tier": "read", "resource": "audit_log"}})
}

# ---- region pinning (§12) ----------------------------------------------------

test_cross_region_denied_by_default if {
	authz.decision.effect == "deny" with input as object.union(read_input, {"region": "US"})
}

test_cross_region_allowed_with_legal_basis if {
	authz.decision.effect == "allow" with input as object.union(read_input, {"region": "US", "cross_region_legal_basis": true})
}

test_missing_residency_denied if {
	# json.patch removes the nested key (object.union deep-merges and would keep it).
	no_residency := json.patch(read_input, [{"op": "remove", "path": "/user/residency"}])
	authz.decision.effect == "deny" with input as no_residency
}

# ---- classification fail-safe (§24): missing/unknown => Restricted ----------

# A read with NO classification must not be a plain allow — it escalates to
# Restricted and is blocked pending step-up + human approval.
test_missing_classification_blocks_plain_read if {
	d := authz.decision with input as object.remove(read_input, {"data_classification"})
	d.effect == "pending_approval"
}

test_unknown_classification_treated_as_restricted if {
	d := authz.decision with input as object.union(read_input, {"data_classification": "wat"})
	d.effect == "pending_approval"
}

# Restricted is reachable only with a valid context-bound approval (explicit grant).
test_restricted_read_allowed_only_with_approval if {
	restricted_read := object.union(read_input, {
		"data_classification": "PHI",
		"request": {"context_hash": "abc123"},
		"approval": {
			"step_up_verified": true,
			"human_approved": true,
			"bound_context_hash": "abc123",
			"single_use": true,
			"not_expired": true,
		},
	})
	d := authz.decision with input as restricted_read
	d.effect == "allow"
	{"action": "redact", "scope": "restricted"} in d.obligations
}

# ---- high-risk tier: step-up + human approval (§7) ---------------------------

test_write_without_approval_is_pending if {
	d := authz.decision with input as object.remove(approved_write, {"approval"})
	d.effect == "pending_approval"
	"require_step_up" in d.obligations
	"require_human_approval" in d.obligations
}

test_write_with_valid_bound_approval_allowed if {
	authz.decision.effect == "allow" with input as approved_write
}

# Approval bound to a DIFFERENT request context must not authorize this one (§15).
test_write_with_mismatched_context_is_pending if {
	tampered := object.union(approved_write, {"request": {"context_hash": "DIFFERENT"}})
	authz.decision.effect == "pending_approval" with input as tampered
}

test_write_missing_step_up_is_pending if {
	weak := object.union(approved_write, {"approval": object.union(approved_write.approval, {"step_up_verified": false})})
	authz.decision.effect == "pending_approval" with input as weak
}

# ---- obligations -------------------------------------------------------------

# PCI is Restricted, so it needs approval AND carries the tokenize obligation.
test_pci_allow_carries_tokenize_obligation if {
	pci := object.union(read_input, {
		"data_classification": "PCI",
		"request": {"context_hash": "abc123"},
		"approval": {
			"step_up_verified": true,
			"human_approved": true,
			"bound_context_hash": "abc123",
			"single_use": true,
			"not_expired": true,
		},
	})
	d := authz.decision with input as pci
	d.effect == "allow"
	{"action": "tokenize", "field": "pan"} in d.obligations
}

test_bulk_allow_carries_cap_rows_obligation if {
	bulk := object.union(approved_write, {"tool": {"tier": "bulk", "resource": "orders"}})
	d := authz.decision with input as bulk
	d.effect == "allow"
	{"action": "cap_rows", "limit": 1000} in d.obligations
}
