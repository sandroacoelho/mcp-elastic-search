---
title: Software Supply Chain Security
doc_id: supply-chain-security
version: 1.0
date: 2026-03-29
status: accepted
owner: security architecture
audience: [architects, security, ai-assistants]
regimes: []
tags: [supply-chain, security, ci-cd, sbom, slsa, sigstore, containers, github-actions, c-scrm]
summary: >
  Mandatory software supply-chain security practices for each blueprint clone: SHA-pinned
  GitHub Actions, digest-pinned container images, SCA and SBOM for dependency
  visibility, and hardened CI/CD pipelines. Driven by real-world incidents
  (SolarWinds, Log4Shell, the March 2026 Trivy compromise) and aligned to the
  NIST SSDF and SLSA frameworks.
---

# Software Supply Chain Security

> **Status:** Accepted v1.0 · **Date:** 2026-03-29
> **Context:** Reducing the risk that a compromised third-party link injects
> malicious code into the final product.

---

## 1. Context & Problem

Software supply-chain attacks are no longer a theoretical risk — they sit at the
center of the industry's biggest security crises. Incidents such as SolarWinds,
Log4Shell, and the Trivy compromise (March 2026) have shown how vulnerabilities in
third parties can escalate into massive impact across thousands of organizations
simultaneously.

The software supply chain encompasses everything that goes into the product: open
source libraries, container images, plugins, build tools, CI/CD pipelines, SaaS
providers, and automation scripts maintained by third parties. The risk is
distributed across multiple actors — library maintainers, cloud providers,
repository platforms, CI/CD providers, image distributors — and the goal is to
reduce the probability that a compromised link, even one outside our direct
control, can inject malicious code into the final product.

### Reference real-world incidents

**SolarWinds** — Attackers compromised the Orion build process, injecting a
backdoor directly into the binary distributed to customers. It exploited trust in
a legitimate update, reaching government agencies and large enterprises at global
scale. Lessons: the build pipeline is as critical a target as production;
artifact signing and independent integrity verification could have detected the
unauthorized changes.

**Log4Shell (Log4j)** — Exposed how a single open source dependency can compromise
vast Java ecosystems within days. The biggest challenge was not applying the patch
but discovering where Log4j was embedded, directly or transitively. Lessons: SBOM
moved from an optional concept to a strategic requirement; SCA tooling and
dependency-inventory automation became essential.

**Trivy (March 2026)** — One of the most widely used container vulnerability
scanners suffered a supply-chain attack that compromised both its binary and its
associated GitHub Actions. A malicious actor manipulated tags and SHAs in the
actions repositories, published version v0.69.4 with a credential-stealing
payload, and distributed malicious images across multiple registries (GitHub,
Docker Hub, ECR, deb/rpm packages). Projects that referenced actions by generic
tags (`@v0`, `@v0.12`), used images without a digest, or downloaded binaries
without integrity validation were automatically exposed. Environments that pinned
actions and images by full SHA did not execute the altered artifacts.

### Most common attack vectors

- **Compromised or malicious dependencies** — malicious npm, PyPI, Maven, or NuGet
  packages; typosquatting; takeover of abandoned packages.
- **Manipulated CI/CD pipelines and Actions** — modifications to workflows,
  actions, and runners to inject malicious logic, capture secrets, or alter
  artifacts during the build.
- **Insecure container images and registries** — use of public images without
  verification, mutable tags (`latest`, `ubuntu:24.04`), lack of image signing.
- **Build and distribution tooling** — compromise of build systems, plugins,
  release pipelines, and binary repositories.
- **Lack of visibility and inventory** — no consolidated view of libraries,
  transitive dependencies, and third-party components.

## 2. Decision

We adopt the following supply-chain security practices as mandatory for any
project cloned from this blueprint.

### 1. GitHub Actions pinned by commit SHA

Every action referenced in workflows MUST use the full commit SHA, never mutable
tags.

Insecure:
```yaml
uses: actions/checkout@v4
```

Secure:
```yaml
uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4.3.1
```

Complementary practices:
- Restrict `GITHUB_TOKEN` permissions to the minimum necessary.
- Avoid unknown or poorly maintained actions; prefer official, audited
  repositories.
- Pin the versions of tools installed in the pipeline (e.g. Trivy, kubectl,
  Terraform) and validate checksums.

### 2. Docker images pinned by digest (SHA256)

Every Docker image referenced in a `Dockerfile` or `docker-compose.yml` MUST use
an immutable digest.

Insecure:
```dockerfile
FROM ubuntu:24.04
```

Secure:
```dockerfile
FROM ubuntu:noble-20260217@sha256:186072bba1b2f436cbb91ef2567abca677337cfc786c86e107d25b7072feef0c
```

Complementary practices:
- Maintain private registries with audited base images.
- Sign images (Sigstore/Cosign) and apply admission policies that reject unsigned
  images.
- Run vulnerability scanners (Trivy, Clair, Grype) integrated into the pipeline —
  ensuring the integrity of the scanner itself as well.

### 3. Dependency management and SBOM

- Integrate SCA (Software Composition Analysis) to monitor vulnerabilities in
  dependencies (Snyk, Dependabot, OWASP Dependency-Check).
- Keep an up-to-date SBOM (Software Bill of Materials) for critical services,
  integrating its generation into the build pipeline.
- Avoid unaudited or poorly maintained dependencies; assess project health
  (activity, community, governance).

### 4. CI/CD pipeline protection

- Least privilege on tokens — each job has only the permissions it needs.
- Dedicated runners where possible; auditable logs.
- Periodically review pipelines, permissions, secrets, and external integrations.
- Automate integrity checks (hashes, signatures, policy-as-code in admission
  controllers).

### 5. Reference frameworks

| Framework | Description |
|-----------|-------------|
| **NIST SSDF** (Secure Software Development Framework) | Guides secure practices across the entire development lifecycle, including dependency management, pipeline protection, and integrity controls. |
| **SLSA** (Supply-chain Levels for Software Artifacts) | A tiered model defining increasing requirements for integrity and provenance, from reproducible builds through hermetic pipelines and robust artifact signing. |
| **SBOM** (Software Bill of Materials) | A structured inventory of all software components, enabling rapid vulnerability tracing, audits, and compliance. |

Organizations that combine SSDF, SLSA, and SBOM, together with a formal Cyber
Supply Chain Risk Management (C-SCRM) program, can significantly reduce the impact
of new incidents.

## 3. Alternatives Considered

Not applicable — recorded as accepted practice driven by industry incidents and
established frameworks (NIST SSDF, SLSA); alternative approaches were not formally
evaluated in the source decision.

## 4. Consequences

### Positive
- Drastic reduction of the attack surface from mutable components (tags, actions,
  images).
- Full dependency traceability via SBOM.
- Early detection of dependency vulnerabilities via automated SCA.
- Alignment with recognized frameworks (NIST SSDF, SLSA).
- Immutability principle: the fewer mutable elements between commit and
  production, the smaller the surface available to an attacker.

### Negative / Trade-offs
- Updating actions and images by SHA takes more effort than simply using tags.
- Generating and maintaining an SBOM adds a step to the pipeline.
- SCA tools can produce false positives that require triage.

## 5. Threat Model

Derived from the attack vectors in §1; each vector is mitigated by the controls in
§2.

| # | Threat | Vector | Primary control(s) |
|---|--------|--------|--------------------|
| T1 | Compromised / malicious dependency | Malicious npm/PyPI/Maven/NuGet packages, typosquatting, abandoned-package takeover | SCA + SBOM + dependency health vetting (§2.3) |
| T2 | Manipulated CI/CD pipeline or Action | Altered workflows/actions/runners injecting logic, capturing secrets, or tampering artifacts | SHA-pinned actions + least-privilege `GITHUB_TOKEN` + pipeline review (§2.1, §2.4) |
| T3 | Insecure container image / registry | Unverified public images, mutable tags, unsigned images | Digest-pinned images + Sigstore/Cosign signing + admission control (§2.2) |
| T4 | Build / distribution tooling compromise | Compromised build systems, plugins, release pipelines, binary repos (e.g. the Trivy incident) | Pinned tool versions + checksum validation + signed artifacts + integrity checks (§2.1, §2.2, §2.4) |
| T5 | Lack of visibility / inventory | No consolidated view of direct and transitive components | SBOM generation in the build pipeline (§2.3) |

## 6. Compliance Control Mapping

Not applicable — this ADR declares no regulatory regime. It aligns to the
non-regulatory **NIST SSDF** and **SLSA** frameworks (see §2.5) rather than to a
compliance regime in the repo's `regimes` taxonomy.

## 7. Open Items / Next Decisions

- [ ] Reconcile this ADR's number with the source draft, which was labeled both
  ADR-013 (filename) and ADR-014 (title); filed here as ADR-0002 per repo
  convention.
- [ ] Decide a target SLSA level for critical services.
- [ ] Select the SBOM format and generation tooling (e.g. CycloneDX vs. SPDX).

---

## Appendix — AI Guidance

Concrete do/don't rules for AI assistants working in this repository.

### DO
- When creating or editing GitHub Actions workflows, ALWAYS pin actions by full
  commit SHA with a version comment (e.g. `# v4.3.1`).
- When creating or editing Dockerfiles, ALWAYS use images with a SHA256 digest.
- When adding dependencies (Maven, npm), verify the package is actively maintained
  and has no known security alerts.
- When creating CI/CD pipelines, apply minimum permissions to `GITHUB_TOKEN`.
- Include a vulnerability-scan step (Trivy, Snyk, Dependabot) in workflows.

### DON'T
- Don't reference GitHub Actions by mutable tag (`@v4`, `@main`, `@latest`).
- Don't use Docker images by tag without a digest (`FROM node:20`, `FROM postgres:18`).
- Don't add dependencies without assessing project health and maintenance.
- Don't grant broad permissions (`permissions: write-all`) in workflows.
- Don't ignore Dependabot or SCA alerts.

## Appendix — References

- [NIST SSDF (SP 800-218)](https://csrc.nist.gov/pubs/sp/800/218/final)
- [SLSA — Supply-chain Levels for Software Artifacts](https://slsa.dev/)
- [Sigstore / Cosign](https://www.sigstore.dev/)
- [CycloneDX SBOM](https://cyclonedx.org/) · [SPDX](https://spdx.dev/)
- [Aqua Security Trivy advisory GHSA-69fq-xp46-6x23](https://github.com/aquasecurity/trivy/security/advisories/GHSA-69fq-xp46-6x23)
- [Aqua Security Trivy supply-chain incident update](https://www.aquasec.com/blog/trivy-supply-chain-attack-what-you-need-to-know/)
