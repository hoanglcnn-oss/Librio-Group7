# Librio — Sprint 4 Software Requirements Specification

## 1. Purpose and Scope

Sprint 4 extends the existing Librio discovery and circulation workflows with:
- physical-copy inventory management;
- librarian Collection Cockpit;
- paid membership;
- membership borrowing quota;
- membership-aware digital access;
- ISBN-assisted metadata entry.

Out of scope:
- production payment gateway;
- DRM and digital rental;
- advanced analytics/reporting;
- bulk inventory operations.

---

## 2. Functional Requirements

### US-14 — Physical-copy Inventory Management

**Requirement:**  
Librarians shall manage individual physical copies with stable identity, location and inventory state while preserving circulation integrity.

**Acceptance Criteria:**
- Each copy has a unique barcode and required location.
- Inventory state is separate from circulation state.
- Only `ACTIVE + AVAILABLE` copies are borrowable.
- Active `RESERVED` or `BORROWED` copies cannot be withdrawn, lost or damaged through inventory administration.
- Physical-copy removal uses `WITHDRAWN` instead of hard deletion.
- Existing circulation history remains valid after inventory changes.

**Business Rules:**
- `resourceId` identifies a bibliographic resource; `barcode` identifies a physical copy.
- Borrowability is server-derived.
- Overdue remains derived from borrowing timestamps, not stored as item state.

---

### US-15 — Collection Cockpit

**Requirement:**  
The system shall provide librarians with a consolidated operational view of physical inventory and circulation state.

**Acceptance Criteria:**
- Librarians can view inventory summary values.
- Librarians can search and filter physical copies.
- Cockpit results include server-derived borrowability, attention state and active circulation context.
- Backend handles pagination and stable ordering.
- Cockpit reuses existing inventory and circulation workflows.

**Business Rules:**
- Summary and attention state are server-derived.
- Client must not recompute operational state.
- The same attention predicate must be used by filtering and summary counts.

---

### US-16 — Paid Membership

**Requirement:**  
Eligible readers shall be able to activate membership through the Sprint 4 mock payment lifecycle.

**Acceptance Criteria:**
- Only eligible readers may activate membership.
- Failed payment creates no membership subscription.
- Successful payment creates one membership subscription.
- Effective membership state is `NONE`, `ACTIVE` or `EXPIRED`.
- Competing activation attempts must not create multiple active subscriptions.
- Membership expiry does not modify existing borrowings.

**Business Rules:**
- Payment transaction and membership subscription are separate lifecycles.
- Membership state is derived from `startsAt`, `expiresAt` and server time.
- Backend is authoritative for activation and expiry.

---

### US-17 — Membership Borrowing Quota

**Requirement:**  
The system shall enforce a borrowing quota defined by the active membership plan.

**Acceptance Criteria:**
- Borrow quota is evaluated within each monthly membership cycle.
- Quota usage is based on successful borrowings in the current cycle.
- Active borrow requests reserve quota capacity.
- Rejected, cancelled and expired requests do not consume quota.
- Concurrent requests must not exceed the available quota.
- Quota state is server-derived.

**Business Rules:**
- Monthly cycle is calculated from `membership.startsAt`.
- Returned borrowings still count as used quota.
- Quota does not carry over between cycles.

---

### US-18 — Membership-aware Digital Access

**Requirement:**  
The system shall provide preview digital access to visitors and full content access only to active members.

**Acceptance Criteria:**
- Visitors and non-members can access preview content where configured.
- Active members can access full content.
- Full-content access is checked by the backend at request time.
- Direct URL access must not bypass membership checks.
- Preview and full content references are separate and server-owned.

**Business Rules:**
- Content references do not represent entitlement.
- Membership entitlement is derived from effective membership state.
- DRM and per-reader digital rental are outside Sprint 4 scope.

---

### US-19 — Google Books ISBN Metadata Lookup

**Requirement:**  
Librarians shall be able to look up book metadata by ISBN and use matching Google Books data as editable Resource prefill.

**Acceptance Criteria:**
- ISBN-10/13 is validated and normalized.
- Google Books is called by the backend.
- Only exact normalized ISBN matches are accepted.
- Metadata is prefilled but not automatically persisted.
- Librarians may edit prefilled data before saving.
- Manual Resource entry remains available if lookup fails.

**Business Rules:**
- Google Books is an external metadata provider, not the source of truth.
- Resource persistence always requires explicit librarian action.
- Metadata provenance is stored.

---

## 3. Non-functional Requirements

- Authorization is enforced by the backend.
- Business-critical mutations must preserve transactional consistency.
- Concurrent operations must not violate circulation, membership or quota invariants.
- Time-dependent state is derived using server time.
- Sprint 1–3 workflows must remain backward compatible unless explicitly changed.

---

## 4. Traceability

| User Story | Design | Verification |
|---|---|---|
| US-14 | Sprint 4 inventory LLD / API contract | T-147 |
| US-15 | Sprint 4 Cockpit LLD / API contract | T-156 |
| US-16 | Sprint 4 membership LLD / API contract | T-167 |
| US-17 | Sprint 4 quota LLD / API contract | T-176 |
| US-18 | Sprint 4 digital-access LLD / API contract | T-187 |
| US-19 | Sprint 4 Google Books LLD / API contract | T-198 |