# Tests for the Elasticsearch read-only server's authorization (ADR-0003), layered
# on the generic PDP in authz.rego. They assert the read-tier tools (listIndices,
# describeIndex, search, getDocument, count) authorize correctly against the index
# allowlist (= role_grants resources) and that read-tier obligations — result cap
# and sensitive-field projection — ride with every allow.
#
#   opa test policy/ -v
#
# Fixtures use data.json: role "es_reader" grants indices "products" and "customers";
# valid purpose "catalog_search"; read_row_cap = 100.
package mcp.authz_elastic_test

import rego.v1

import data.mcp.authz

# A baseline read-tier ES call: allowlisted index, valid role/purpose, same region,
# low-sensitivity data.
es_read := {
	"user": {"role": "es_reader", "residency": "EU"},
	"region": "EU",
	"tool": {"tier": "read", "resource": "products"},
	"purpose": "catalog_search",
	"data_classification": "internal",
}

bound_approval := {
	"step_up_verified": true,
	"human_approved": true,
	"bound_context_hash": "ctx-1",
	"single_use": true,
	"not_expired": true,
}

# ---- happy path --------------------------------------------------------------

test_es_search_allowed if {
	authz.decision.effect == "allow" with input as es_read
}

test_es_read_carries_row_cap_obligation if {
	d := authz.decision with input as es_read
	{"action": "cap_rows", "limit": 100} in d.obligations
}

# Low-class reads are NOT field-projected (no over-restriction).
test_es_low_class_read_not_projected if {
	d := authz.decision with input as es_read
	not {"action": "project_source", "scope": "minimal"} in d.obligations
}

# ---- allowlist scoping (threat E2) -------------------------------------------

test_es_unlisted_index_denied if {
	off_list := object.union(es_read, {"tool": {"tier": "read", "resource": "secret_index"}})
	authz.decision.effect == "deny" with input as off_list
}

test_es_invalid_purpose_denied if {
	authz.decision.effect == "deny" with input as object.union(es_read, {"purpose": "snooping"})
}

# ---- region pinning (§12) ----------------------------------------------------

test_es_cross_region_denied if {
	authz.decision.effect == "deny" with input as object.union(es_read, {"region": "US"})
}

# ---- minimum-necessary on sensitive reads (threat E4) ------------------------

# Confidential (PII) read needs no approval but is field-projected and row-capped.
test_es_confidential_read_projects_source if {
	conf := object.union(es_read, {"data_classification": "PII"})
	d := authz.decision with input as conf
	d.effect == "allow"
	{"action": "project_source", "scope": "minimal"} in d.obligations
	{"action": "cap_rows", "limit": 100} in d.obligations
}

# ---- restricted index data: fail closed unless approved (§24, §7) ------------

test_es_restricted_read_pending_without_approval if {
	phi := object.union(es_read, {"data_classification": "PHI"})
	authz.decision.effect == "pending_approval" with input as phi
}

test_es_restricted_read_allowed_with_bound_approval if {
	phi := object.union(es_read, {
		"data_classification": "PHI",
		"request": {"context_hash": "ctx-1"},
		"approval": bound_approval,
	})
	d := authz.decision with input as phi
	d.effect == "allow"
	{"action": "redact", "scope": "restricted"} in d.obligations
	{"action": "project_source", "scope": "minimal"} in d.obligations
}
