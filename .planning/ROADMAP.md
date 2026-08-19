# Roadmap: Librio

## Overview

Librio's MVP still spans discovery, borrowing, return/overdue, and digital access, but the active roadmap here tracks the current Sprint 1 milestone only. Sprint 1 is a local walking skeleton that proves browse/search, detail, and availability against the real backend before any future backlog work is pulled forward.

**Current milestone:** Sprint 1
**Phase 1 runtime target:** Clean-clone local Spring Boot + React/Vite + PostgreSQL environment; frontend consumes the real backend API with fixed seed data, documented commands, and test/evidence output.
**Canonical Phase 1 API contract:** docs/lld/api-contracts/sprint-1-api.md

## Phases

**Phase Numbering:**

- Integer phases 1, 2, 3, 4: Planned milestone work

- [x] **Phase 1: Local Walking Skeleton & Seed** - Spring Boot, React/Vite, and PostgreSQL start locally with the resource/physical-item/digital-item model and fixed four-case seed data.
- [ ] **Phase 2: Browse & Search Slice** - Readers can browse the default catalog and run trimmed keyword search with URL state and clear loading, empty, and error behavior.
- [ ] **Phase 3: Detail & Availability Slice** - Readers can open resource detail, see access types and server-derived availability, and receive a 404 for unknown resources.
- [ ] **Phase 4: Review Candidate & Evidence** - The local clone, backend/frontend builds and tests, demo-path API checks, README/curl/QA evidence, and secret-free source state are all reproducible.

## Phase Details

### Phase 1: Local Walking Skeleton & Seed
**Goal**: Stand up the minimal local stack and seed model required to prove the Sprint 1 path end to end.
**Depends on**: Nothing (first phase)
**Requirements**: LIB-05
**Success Criteria** (what must be TRUE):
  1. Spring Boot, React/Vite, and PostgreSQL start locally from documented commands.
  2. The resource, physical-item, and digital-item model exists in the running stack.
  3. Fixed four-case seed data is available.
  4. A minimal frontend-to-real-backend tracer path is working end to end.
**Plans**: 2/2 plans complete (`01-01-PLAN.md`, `01-02-PLAN.md`)
**UI hint**: yes

### Phase 2: Browse & Search Slice
**Goal**: Readers can browse the default catalog and search with a trimmed keyword in a URL-driven flow.
**Depends on**: Phase 1
**Requirements**: LIB-01, LIB-02
**Success Criteria** (what must be TRUE):
  1. A reader can browse the default catalog without a search term.
  2. A reader can submit a trimmed keyword search and keep the query in the URL.
  3. Result, empty, loading, and error states are all visible and distinct.
  4. Search uses `GET /resources?q={keyword}`.
**Plans**: TBD
**UI hint**: yes

### Phase 3: Detail & Availability Slice
**Goal**: Readers can inspect a resource and see server-derived access and availability data.
**Depends on**: Phase 2
**Requirements**: LIB-03, LIB-04, LIB-06
**Success Criteria** (what must be TRUE):
  1. A reader can open a resource detail view and receive a not-found result for an unknown resource.
  2. Resource detail shows access types plus server-derived physical and digital availability.
  3. The phase covers physical-only, out-of-stock, digital-only, and mixed cases.
  4. The complete Sprint 1 API contract is implemented.
**Plans**: TBD
**UI hint**: yes

### Phase 4: Review Candidate & Evidence
**Goal**: Produce the evidence package that proves Sprint 1 works from a clean clone.
**Depends on**: Phase 3
**Requirements**: ENV-01
**Success Criteria** (what must be TRUE):
  1. A clean clone starts the documented local stack and reaches the real API on the complete demo path.
  2. Backend and frontend builds and tests pass.
  3. README, curl responses, QA matrix, D02 Actual, and demo evidence are available.
  4. Source is pushed without secrets or build artifacts.
**Plans**: TBD
**UI hint**: yes

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Local Walking Skeleton & Seed | 2/2 | Complete | 2026-08-19 |
| 2. Browse & Search Slice | 0/TBD | Not started | - |
| 3. Detail & Availability Slice | 0/TBD | Not started | - |
| 4. Review Candidate & Evidence | 0/TBD | Not started | - |
