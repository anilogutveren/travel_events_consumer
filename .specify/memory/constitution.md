<!--
SYNC IMPACT REPORT
==================
Version change: 0.0.0 (template) → 1.0.0 (initial ratification)
Modified principles: N/A (initial creation)
Added sections:
  - I. Code Quality
  - II. Test-First Development
  - III. Testing Standards
  - IV. User Experience Consistency
  - V. Performance Requirements
  - Performance Benchmarks & Budgets (Section 2)
  - Development Workflow & Quality Gates (Section 3)
  - Governance
Templates requiring updates:
  ✅ .specify/templates/plan-template.md — Constitution Check gates align with principles I–V
  ✅ .specify/templates/spec-template.md — Success Criteria must include performance metrics (SC refs)
  ✅ .specify/templates/tasks-template.md — Polish phase must include observability, perf, and UX tasks
Deferred TODOs:
  - TODO(RATIFICATION_DATE): Confirm exact project start date; set to 2026-06-07 (today) as initial ratification.
-->

# Travel Events Consumer Constitution

## Core Principles

### I. Code Quality

Every piece of code merged into the main branch MUST be readable, maintainable, and
purposeful. This means:

- Functions and methods MUST have a single, clearly named responsibility.
- Cyclomatic complexity per function MUST NOT exceed 10; violations require explicit
  justification in the PR description.
- Dead code, commented-out blocks, and unused imports MUST be removed before merge.
- All public interfaces MUST be named to reflect intent without requiring a comment
  to understand their purpose.
- Dependencies MUST be introduced deliberately: every new dependency requires a stated
  rationale. Transitive dependency bloat is a violation of this principle.
- Code duplication across more than two call sites MUST be extracted into a shared
  utility; exceptions must be documented.

**Rationale**: Unreadable code accrues hidden maintenance debt. In an event-driven
consumer service, subtle logic errors are amplified at scale — clarity is a safety net.

### II. Test-First Development (NON-NEGOTIABLE)

Tests MUST be written before implementation code. The Red-Green-Refactor cycle is
mandatory for all functional changes:

1. Write a failing test that describes the desired behaviour.
2. Obtain explicit confirmation that the test fails for the right reason.
3. Write the minimum implementation to make the test pass.
4. Refactor without breaking the green state.

Skipping straight to implementation is a constitution violation. No PR may be merged
where implementation commits precede their corresponding test commits (verifiable via
git history).

**Rationale**: Test-first forces precise specification of behaviour before code exists,
catching design flaws early and producing a living executable specification of the
system.

### III. Testing Standards

The project MUST maintain a layered test suite covering unit, integration, and contract
levels:

- **Unit tests**: MUST cover every pure function and domain rule. Target ≥ 90 % line
  coverage for `src/` excluding generated code and main entry points.
- **Integration tests**: MUST exist for every external boundary (message broker,
  database, downstream HTTP service). Integration tests MUST run against real
  infrastructure (Docker Compose or equivalent) — mocks at the integration layer are
  forbidden unless the external system is third-party and unavailable locally.
- **Contract tests**: MUST be written for every event schema consumed or produced.
  Schema drift that breaks a contract test is a blocking issue; it MUST NOT be merged.
- Tests MUST be deterministic and side-effect-free. Flaky tests MUST be triaged and
  fixed within one sprint; they MUST NOT be skipped indefinitely.
- CI MUST enforce the full test suite on every PR. A red pipeline blocks merge.

**Rationale**: In an event consumer, correctness at the schema and integration boundary
is more critical than internal unit coverage. Contract tests are the primary defence
against silent data corruption.

### IV. User Experience Consistency

Every user-facing surface (API responses, CLI output, error messages, logs) MUST adhere
to a single consistent style:

- Error responses MUST follow a uniform envelope: `{ "error": { "code": "...",
  "message": "..." } }`. Ad-hoc error shapes are forbidden.
- Log messages MUST be structured (JSON in production), carry a `correlation_id`, and
  use consistent severity levels (`DEBUG`, `INFO`, `WARN`, `ERROR`). Free-form
  `print`-style logging is forbidden in production paths.
- CLI output (if applicable) MUST distinguish informational output (stdout) from
  errors (stderr) consistently.
- Field naming MUST follow a single casing convention (snake_case for JSON payloads,
  established at project start) and MUST NOT be mixed within a single API version.
- Human-readable messages MUST be written for the operator, not the developer: they
  MUST state what happened, what the impact is, and what action is required.

**Rationale**: Inconsistent interfaces erode operator trust, complicate monitoring, and
slow incident response. Structured consistency is especially critical in event pipelines
where logs and events are parsed programmatically.

### V. Performance Requirements

The system MUST meet measurable performance targets; aspirational language ("fast",
"efficient") is not acceptable in requirements or acceptance criteria:

- Event processing throughput MUST be defined per feature spec in terms of
  events/second with an explicit p99 latency budget.
- End-to-end processing latency (event ingestion → side-effect committed) MUST be
  within the budget defined in the feature spec. If unspecified, the default budget
  is **p99 ≤ 500 ms** for synchronous paths and **p99 ≤ 5 s** for async/batch paths.
- Memory footprint MUST NOT grow unboundedly; any stateful component MUST document
  its eviction or GC strategy.
- Performance regressions ≥ 10 % on any tracked benchmark MUST be flagged in the PR
  and require explicit sign-off before merge.
- Load and soak tests MUST be run before any release that changes a hot processing
  path.

**Rationale**: Event consumers are often on the critical path for downstream services.
Undefined performance contracts are a production risk; explicit, testable budgets make
regressions visible before they reach production.

## Performance Benchmarks & Budgets

This section records the canonical numeric targets that Principle V refers to. Update
these values when architectural decisions change the feasible operating range.

| Metric | Default Budget | Override Location |
|---|---|---|
| Synchronous p99 latency | ≤ 500 ms | Feature spec `Success Criteria` |
| Async/batch p99 latency | ≤ 5 s | Feature spec `Success Criteria` |
| Throughput baseline | ≥ 100 events/s single instance | Feature plan `Performance Goals` |
| Memory ceiling (single process) | ≤ 512 MB resident | Feature plan `Constraints` |
| Regression threshold | 10 % degradation triggers review | CI benchmark gate |

Performance budgets MUST be measured under representative load using production-like
data shapes, not synthetic micro-benchmarks alone.

## Development Workflow & Quality Gates

Every change moving toward the main branch MUST pass through these gates in order:

1. **Constitution Check** — Reviewer confirms no principle is violated. Violations
   require documented justification in the PR or must be resolved before merge.
2. **Test Gate** — All tests green in CI (unit + integration + contract). Coverage
   thresholds enforced by CI tooling.
3. **Performance Gate** — If the change touches a hot path, benchmark results MUST be
   attached to the PR. A regression ≥ 10 % is blocking.
4. **Review Gate** — At least one peer review approval. Reviewer is responsible for
   verifying UX consistency (Principle IV) and code quality (Principle I).
5. **Changelog/Observability Gate** — Structured log entries and correlation IDs are
   present for any new processing path.

Branch strategy: all feature work on short-lived branches (`###-feature-name`), merged
via PR. Direct commits to `main` are forbidden except for emergency hotfixes, which
require retrospective documentation within 24 hours.

## Governance

This constitution supersedes all prior informal practices and README-level guidelines.
In any conflict between this document and a ticket, PR description, or verbal agreement,
the constitution governs.

**Amendment procedure**:
- Amendments MUST be proposed as a PR modifying this file.
- Minor amendments (new guidance, expanded rationale) require one approver.
- Major amendments (principle removal, redefinition of non-negotiable rules) require
  consensus of all active maintainers and a migration plan for existing code.
- The `CONSTITUTION_VERSION` MUST be incremented according to semantic versioning:
  MAJOR for backward-incompatible governance changes, MINOR for new/expanded
  principles, PATCH for clarifications.

**Compliance review**: Each sprint retrospective SHOULD include a brief check on
whether any principle was violated or found unworkable. If a principle cannot be
followed in practice, it MUST be amended rather than silently ignored.

**Runtime guidance**: Refer to `CLAUDE.md` (project root) for agent-specific
development conventions and tool configuration.

**Version**: 1.0.0 | **Ratified**: 2026-06-07 | **Last Amended**: 2026-06-07
