# Roadmap: Librio

## Overview

Librio’s MVP proceeds from a reproducible reader-discovery slice to an account-backed physical lending workflow, then completes the return/overdue lifecycle and adds digital access with the minimum librarian operations that sustain the catalog. Phase 1 is the active Sprint 1 rescue/review milestone, and its success is a stable local full-stack demonstration rather than public deployment.

**Current milestone:** Sprint 1 — Browse/Search → Resource Detail → Availability  
**Phase 1 runtime target:** Clean-clone local Spring Boot + React/Vite + PostgreSQL environment; frontend consumes the real backend API with fixed seed data, documented commands, and test/evidence output.  
**Canonical Phase 1 API contract:** docs/lld/api-contracts/sprint-1-api.md

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions, if needed after planning

- [ ] **Phase 1: Local Reader Discovery Demo** - Readers can browse, search, inspect resources, and see real availability in a reproducible local full-stack demo.
- [ ] **Phase 2: Account-Backed Physical Borrowing** - An identified reader can safely borrow an available physical item with an authoritative due date and availability update.
- [ ] **Phase 3: Return & Overdue Lifecycle** - The physical lending workflow correctly handles returns and makes overdue loans visible.
- [ ] **Phase 4: Digital Access & Library Operations** - Readers access digital resources while librarians maintain the minimum catalog and circulation data.

## Phase Details

### Phase 1: Local Reader Discovery Demo
**Goal**: Readers can reliably discover a resource and see its true access and availability through a local end-to-end application that a developer can run from a clean clone.
**Depends on**: Nothing (first phase)
**Requirements**: LIB-01, LIB-02, LIB-03, LIB-04, LIB-05, LIB-06, ENV-01
**Success Criteria** (what must be TRUE):
  1. From a clean clone, a developer can follow documented local commands to start PostgreSQL, the Spring Boot backend, and the React/Vite frontend with fixed seed data, then demonstrate Browse/Search → Resource Detail → Availability through the real API.
  2. A reader can browse resource cards, select one, and reach its detail page with title, authors, and description.
  3. A reader can submit a trimmed search keyword, see the query reflected in the URL, receive matching cards or a clear empty state, and browse normally when the query is blank.
  4. A reader can see the server-provided physical copy counts and/or digital availability for physical-only, digital-only, mixed, and out-of-stock seed cases, without a misleading missing access block.
  5. A reader can distinguish loading, API/network error with retry, and unknown-resource not-found states; tests and demo evidence confirm the documented contract behavior.
**Plans**: TBD
**UI hint**: yes

### Phase 2: Account-Backed Physical Borrowing
**Goal**: An identified reader can borrow an available physical item and receive a trustworthy, server-confirmed loan outcome.
**Depends on**: Phase 1
**Requirements**: ACC-01, LIB-07
**Success Criteria** (what must be TRUE):
  1. A reader can identify their account before starting a physical borrowing workflow, and the created loan is associated with that reader.
  2. A reader can borrow an available physical item and receives a confirmation that includes the due date.
  3. After a successful borrow, readers see the resource’s updated availability, and a second active loan for the same physical item is not granted.
**Plans**: TBD
**UI hint**: yes

### Phase 3: Return & Overdue Lifecycle
**Goal**: Physical loans can be returned and overdue status is visible without violating the item lifecycle.
**Depends on**: Phase 2
**Requirements**: LIB-08, LIB-09
**Success Criteria** (what must be TRUE):
  1. A reader or librarian can complete a return for an active physical loan, after which that item is available to borrow again.
  2. A reader and librarian can view the loan due date and clearly identify a loan that has become overdue.
  3. Resource availability continues to distinguish unavailable borrowed/overdue copies from copies made available by a completed return.
**Plans**: TBD
**UI hint**: yes

### Phase 4: Digital Access & Library Operations
**Goal**: Readers can access available digital resources, and authorized librarians can maintain the operational data needed for the complete MVP workflow.
**Depends on**: Phase 3
**Requirements**: ACC-02, LIB-10, LIB-11
**Success Criteria** (what must be TRUE):
  1. A reader can open or otherwise access an available digital resource from its resource experience, without a digital-expiry workflow.
  2. A librarian can enter the minimum staff operations appropriate to their role rather than exposing those controls to ordinary readers.
  3. A librarian can create or update the resource and physical/digital item data that drives discovery, access type, and availability.
  4. A librarian can maintain the circulation data needed to support the physical borrowing, return, and overdue workflow.
**Plans**: TBD
**UI hint**: yes

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Local Reader Discovery Demo | 0/TBD | Not started | - |
| 2. Account-Backed Physical Borrowing | 0/TBD | Not started | - |
| 3. Return & Overdue Lifecycle | 0/TBD | Not started | - |
| 4. Digital Access & Library Operations | 0/TBD | Not started | - |
